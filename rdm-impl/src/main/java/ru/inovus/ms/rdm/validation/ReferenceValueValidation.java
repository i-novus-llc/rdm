package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.model.version.AttributeFilter;
import ru.inovus.ms.rdm.n2o.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.n2o.model.version.RefBookVersion;
import ru.inovus.ms.rdm.n2o.service.api.VersionService;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.n2o.util.ConverterUtil.castReferenceValue;
import static ru.inovus.ms.rdm.n2o.util.ConverterUtil.field;

/**
 * Проверка конкретного строкового значения на ссылочную целостность
 */
public class ReferenceValueValidation extends ErrorAttributeHolderValidation {

    public static final String REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE = "validation.reference.value.not.found";
    public static final String REFERRED_VERSION_NOT_FOUND_EXCEPTION_CODE = "validation.referred.version.not.found";

    private static final String VERSION_PRIMARY_KEY_NOT_FOUND_EXCEPTION_CODE = "version.primary.key.not.found";

    private final VersionService versionService;

    private final Map<Structure.Reference, String> referenceWithValueMap;

    private Structure structure;

    public ReferenceValueValidation(VersionService versionService,
                                    Map<Structure.Reference, String> referenceWithValueMap,
                                    Structure structure) {
        this.versionService = versionService;
        this.referenceWithValueMap = referenceWithValueMap;
        this.structure = structure;
    }

    public ReferenceValueValidation(VersionService versionService,
                                    Map<Structure.Reference, String> referenceWithValueMap,
                                    Structure structure,
                                    Set<String> excludeAttributes) {
        this(versionService, referenceWithValueMap, structure);
        setErrorAttributes(excludeAttributes);
    }

    public ReferenceValueValidation(VersionService versionService,
                                    Row row,
                                    Structure structure) {
        this(versionService, getReferenceWithValueMap(row, structure), structure);
    }

    public ReferenceValueValidation(VersionService versionService,
                                    Row row,
                                    Structure structure,
                                    Set<String> excludeAttributes) {
        this(versionService, row, structure);
        setErrorAttributes(excludeAttributes);
    }

    @Override
    public List<Message> validate() {
        return referenceWithValueMap.entrySet().stream()
                .filter(entry -> getErrorAttributes() == null || !getErrorAttributes().contains(entry.getKey().getAttribute()))
                .filter(this::isReferenceNotValid)
                .peek(this::addErrorAttribute)
                .map(this::createMessage)
                .collect(Collectors.toList());
    }

    private void addErrorAttribute(Map.Entry<Structure.Reference, String> referenceStringEntry) {
        addErrorAttribute(referenceStringEntry.getKey().getAttribute());
    }

    private Message createMessage(Map.Entry<Structure.Reference, String> entry) {
        return new Message(REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE,
                structure.getAttribute(entry.getKey().getAttribute()).getName(),
                entry.getValue());
    }

    private boolean isReferenceNotValid(Map.Entry<Structure.Reference, String> entry) {
        if (getErrorAttributes().contains(entry.getKey().getAttribute()) || entry.getValue() == null) {
            return false;
        }

        Structure.Reference reference = entry.getKey();
        String referenceValue = entry.getValue();

        RefBookVersion referredVersion;
        try {
            referredVersion = versionService.getLastPublishedVersion(reference.getReferenceCode());

        } catch (NotFoundException e) {
            throw new UserException(new Message(REFERRED_VERSION_NOT_FOUND_EXCEPTION_CODE,
                    reference.getReferenceCode(), reference.getAttribute()), e);
        }
        Integer referredVersionId = referredVersion.getId();
        Structure referredStructure = referredVersion.getStructure();

        Structure.Attribute referredAttribute;
        try {
            referredAttribute = reference.findReferenceAttribute(referredStructure);

        } catch (RdmException e) {
            throw new UserException(new Message(VERSION_PRIMARY_KEY_NOT_FOUND_EXCEPTION_CODE, referredVersionId), e);
        }
        Field referredField = field(referredAttribute);

        Object castedValue = castReferenceValue(referredField, referenceValue);
        AttributeFilter attributeFilter = new AttributeFilter(referredAttribute.getCode(), castedValue, referredAttribute.getType(), SearchTypeEnum.EXACT);
        Set<List<AttributeFilter>> attributeFilters = new HashSet<>();
        attributeFilters.add(singletonList(attributeFilter));

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(attributeFilters, null);
        Page<RefBookRowValue> pagedData = versionService.search(referredVersionId, searchDataCriteria);
        return (pagedData == null || !pagedData.hasContent());
    }

    private static Map<Structure.Reference, String> getReferenceWithValueMap(Row row, Structure structure) {
        Map<Structure.Reference, String> map = new HashMap<>();
        structure.getReferences().stream()
                .filter(reference -> row.getData().get(reference.getAttribute()) != null)
                .forEach(ref -> map.put(ref, ((Reference) row.getData().get(ref.getAttribute())).getValue()));
        return map;
    }

}
