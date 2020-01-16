package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.collections4.MapUtils;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.service.VersionService;

import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.impl.util.ConverterUtil.castReferenceValue;
import static ru.inovus.ms.rdm.impl.util.ConverterUtil.field;

/**
 * Проверка конкретного строкового значения на ссылочную целостность.
 */
public class ReferenceValueValidation extends AppendRowValidation {

    public static final String REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE = "validation.reference.value.not.found";
    public static final String REFERRED_VERSION_NOT_FOUND_EXCEPTION_CODE = "validation.referred.version.not.found";

    private static final String VERSION_PRIMARY_KEY_NOT_FOUND_EXCEPTION_CODE = "version.primary.key.not.found";

    private final VersionService versionService;

    private final Structure structure;
    private final List<Structure.Reference> referenceKeys;
    private final List<String> referenceKeyCodes;

    private List<Map<Structure.Reference, String>> referenceKeyMaps;
    private Map<String, List<RefBookRowValue>> referenceSearchValuesMap;

    public ReferenceValueValidation(VersionService versionService, Structure structure, List<Row> rows) {
        this.versionService = versionService;

        this.structure = structure;
        this.referenceKeys = structure.getReferences();
        this.referenceKeyCodes = referenceKeys.stream().map(Structure.Reference::getAttribute).collect(toList());

        this.referenceKeyMaps = toReferenceKeyMaps(rows);
        this.referenceSearchValuesMap = getRefBookData(rows);
    }

    public ReferenceValueValidation(VersionService versionService, Structure structure, Row row, Set<String> excludeAttributes) {
        this(versionService, structure, singletonList(row), excludeAttributes);
    }

    public ReferenceValueValidation(VersionService versionService, Structure structure, List<Row> rows, Set<String> excludeAttributes) {
        this(versionService, structure, rows);

        setErrorAttributes(excludeAttributes);
    }

    @Override
    public void appendRow(Row row) {
        setErrorAttributes(emptySet());
        super.appendRow(row);
    }

    @Override
    protected List<Message> validate(Long systemId, Map<String, Object> rowData) {

        if (isEmpty(referenceKeyCodes))
            return emptyList();

        return referenceKeys.stream()
                .filter(reference -> !isErrorAttribute(reference.getAttribute()))
                .map(reference -> validate(reference, rowData))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private Message validate(Structure.Reference reference, Map<String, Object> rowData) {

        List<RefBookRowValue> referenceSearchValues = referenceSearchValuesMap.get(reference.getAttribute());
        RowValue refBookRow = !isEmpty(referenceSearchValues) ? findRowValue(reference, rowData, referenceSearchValues) : null;
        if (refBookRow != null)
            return null;

        addErrorAttribute(reference.getAttribute());
        return createMessage(reference.getAttribute(), rowData);
    }

    private List<Map<Structure.Reference, String>> toReferenceKeyMaps(List<Row> rows) {
        return rows.stream().map(this::toReferenceKeyMap).filter(MapUtils::isNotEmpty).collect(toList());
    }

    private Map<Structure.Reference, String> toReferenceKeyMap(Row row) {

        Map<Structure.Reference, String> map = new HashMap<>();
        referenceKeys.stream()
                .filter(reference -> row.getData().get(reference.getAttribute()) != null)
                .forEach(ref -> map.put(ref, toReferenceValue(ref.getAttribute(), row.getData())));
        return map;
    }

    private Map<String, List<RefBookRowValue>> getRefBookData(List<Row> rows) {

        if (isEmpty(referenceKeyMaps))
            return emptyMap();

        return referenceKeys.stream()
                .map(reference -> getRefBookData(reference, rows))
                .filter(Objects::nonNull)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<String, List<RefBookRowValue>> getRefBookData(Structure.Reference reference, List<Row> rows) {

        if (reference == null || reference.isNull())
            return null;

        RefBookVersion referredVersion = getReferredVersion(reference);
        Structure.Attribute referredAttribute = getReferredAttribute(reference, referredVersion);

        SearchDataCriteria searchDataCriteria = createSearchCriteria(reference, rows, referredAttribute);

        Page<RefBookRowValue> pagedData = versionService.search(referredVersion.getId(), searchDataCriteria);
        if (pagedData == null)
            return null;

        List<RefBookRowValue> rowValues = pagedData.getContent().stream()
                .map(rowValue -> toReferredRowValue(referredAttribute.getCode(), rowValue))
                .collect(toList());
        return !isEmpty(rowValues) ? new AbstractMap.SimpleEntry<>(reference.getAttribute(), rowValues) : null;
    }

    private SearchDataCriteria createSearchCriteria(Structure.Reference reference, List<Row> rows, Structure.Attribute referredAttribute) {

        Set<List<AttributeFilter>> attributeFilters = createSearchFilters(reference, rows, referredAttribute);

        SearchDataCriteria criteria = new SearchDataCriteria(attributeFilters, null);
        criteria.setPageNumber(1);
        criteria.setPageSize(rows.size());
        return criteria;
    }

    private Set<List<AttributeFilter>> createSearchFilters(Structure.Reference reference, List<Row> rows, Structure.Attribute referredAttribute) {
        List<String> referenceValues = rows.stream()
                .map(row -> toReferenceValue(reference.getAttribute(), row.getData()))
                .filter(Objects::nonNull)
                .distinct().collect(toList());

        Field referredField = field(referredAttribute);
        List<Object> referredValues = referenceValues.stream()
                .map(referenceValue -> castReferenceValue(referredField, referenceValue))
                .collect(toList());

        return referredValues.stream()
                .map(referredValue -> singletonList(toAttributeFilter(referredAttribute, referredValue)))
                .collect(toSet());
    }

    private RefBookVersion getReferredVersion(Structure.Reference reference) {
        try {
            return versionService.getLastPublishedVersion(reference.getReferenceCode());

        } catch (NotFoundException e) {
            throw new UserException(new Message(REFERRED_VERSION_NOT_FOUND_EXCEPTION_CODE,
                    reference.getReferenceCode(), reference.getAttribute()), e);
        }
    }

    private Structure.Attribute getReferredAttribute(Structure.Reference reference, RefBookVersion referredVersion) {
        try {
            return reference.findReferenceAttribute(referredVersion.getStructure());

        } catch (RdmException e) {
            throw new UserException(new Message(VERSION_PRIMARY_KEY_NOT_FOUND_EXCEPTION_CODE, referredVersion.getId()), e);
        }
    }

    private AttributeFilter toAttributeFilter(Structure.Attribute referredAttribute, Object referredValue) {
        return new AttributeFilter(referredAttribute.getCode(), referredValue, referredAttribute.getType(), SearchTypeEnum.EXACT);
    }

    private RefBookRowValue toReferredRowValue(String attribute, RefBookRowValue rowValue) {

        List<FieldValue> fieldValues = singletonList(rowValue.getFieldValue(attribute));
        rowValue.setFieldValues(fieldValues);

        return rowValue;
    }

    private Message createMessage(String attributeCode, Map<String, Object> rowData) {
        return new Message(REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE,
                structure.getAttribute(attributeCode).getName(), toReferenceValue(attributeCode, rowData));
    }

    /**
     * В списке записей #searchValues ищется строка, которая соответствует строке с данными #rowData
     * на основании значения ссылки #reference.
     *
     * @param reference    атрибут-ссылка на запись
     * @param rowData      строка с данными, для которой ведётся поиск
     * @param searchValues список записей, среди которых ведётся поиск
     * @return Найденная запись либо null
     */
    private RefBookRowValue findRowValue(Structure.Reference reference,
                                         Map<String, Object> rowData,
                                         List<RefBookRowValue> searchValues) {
        return searchValues.stream()
                .filter(searchValue -> {
                    String referenceValue = toReferenceValue(reference.getAttribute(), rowData);
                    List<FieldValue> fieldValues = searchValue.getFieldValues();
                    FieldValue fieldValue = !isEmpty(fieldValues) ? fieldValues.get(0) : null;
                    return referenceValue != null
                            && fieldValue != null
                            && referenceValue.equals(fieldValue.getValue());
                })
                .findFirst().orElse(null);
    }

    private String toReferenceValue(String attributeCode, Map<String, Object> rowData) {
        Object value = rowData.get(attributeCode);
        return (value != null) ? ((Reference) value).getValue() : null;
    }
}
