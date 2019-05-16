package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.util.ConverterUtil.castReferenceValue;
import static ru.inovus.ms.rdm.util.ConverterUtil.field;

/**
 * Проверка конкретного строкового значения на ссылочную целостность
 */
public class ReferenceValueValidation extends ErrorAttributeHolderValidation {

    public static final String REFERENCE_ERROR_CODE = "validation.reference.err";

    private final VersionService versionService;
    private final RefBookVersionRepository versionRepository;

    private final Map<Structure.Reference, String> referenceWithValueMap;

    private Structure structure;

    public ReferenceValueValidation(VersionService versionService,
                                    RefBookVersionRepository versionRepository,
                                    Map<Structure.Reference, String> referenceWithValueMap,
                                    Structure structure) {
        this.versionService = versionService;
        this.versionRepository = versionRepository;
        this.referenceWithValueMap = referenceWithValueMap;
        this.structure = structure;
    }

    public ReferenceValueValidation(VersionService versionService,
                                    RefBookVersionRepository versionRepository,
                                    Map<Structure.Reference, String> referenceWithValueMap,
                                    Structure structure,
                                    Set<String> excludeAttributes) {
        this(versionService, versionRepository, referenceWithValueMap, structure);
        setErrorAttributes(excludeAttributes);
    }

    public ReferenceValueValidation(VersionService versionService,
                                    RefBookVersionRepository versionRepository,
                                    Row row,
                                    Structure structure) {
        this(versionService, versionRepository, getReferenceWithValueMap(row, structure), structure);
    }

    public ReferenceValueValidation(VersionService versionService,
                                    RefBookVersionRepository versionRepository,
                                    Row row,
                                    Structure structure,
                                    Set<String> excludeAttributes) {
        this(versionService, versionRepository, row, structure);
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
        return new Message(REFERENCE_ERROR_CODE,
                structure.getAttribute(entry.getKey().getAttribute()).getName(),
                entry.getValue());

    }

    private boolean isReferenceNotValid(Map.Entry<Structure.Reference, String> entry) {
        if (getErrorAttributes().contains(entry.getKey().getAttribute()) || entry.getValue() == null) {
            return false;
        }

        Structure.Reference reference = entry.getKey();
        String referenceValue = entry.getValue();

        RefBookVersionEntity refBookVersion = versionRepository.findLastVersion(reference.getReferenceCode(), RefBookVersionStatus.PUBLISHED);
        if (refBookVersion == null) return true;
        Integer versionId = refBookVersion.getId();
        Structure referenceStructure = refBookVersion.getStructure();

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(referenceStructure);
        Field fieldFilter = field(referenceAttribute);
        Object referenceValueCasted = castReferenceValue(fieldFilter, referenceValue);
        AttributeFilter attributeFilter = new AttributeFilter(referenceAttribute.getCode(), referenceValueCasted, referenceAttribute.getType(), SearchTypeEnum.EXACT);
        Set<List<AttributeFilter>> attributeFilters = new HashSet<>();
        attributeFilters.add(singletonList(attributeFilter));

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(attributeFilters, null);
        Page<RefBookRowValue> pagedData = versionService.search(versionId, searchDataCriteria);
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
