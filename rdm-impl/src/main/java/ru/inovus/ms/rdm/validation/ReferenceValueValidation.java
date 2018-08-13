package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.util.ConverterUtil.castReferenceValue;
import static ru.inovus.ms.rdm.util.ConverterUtil.field;

/**
 * Проверка конкретного строкового значения на ссылочную целостность
 */
public class ReferenceValueValidation implements RdmValidation {

    public static final String ERROR_CODE = "validation.reference.err";

    private final VersionService versionService;

    private final Map<Structure.Reference, String> referenceWithValueMap;

    private Structure structure;

    private List<String> excludeAttributes;

    public ReferenceValueValidation(VersionService versionService, Map<Structure.Reference, String> referenceWithValueMap, Structure structure, List<String> excludeAttributes) {
        this.versionService = versionService;
        this.referenceWithValueMap = referenceWithValueMap;
        this.structure = structure;
        this.excludeAttributes = excludeAttributes == null ? Collections.emptyList() : excludeAttributes;
    }

    @Override
    public List<Message> validate() {
        return referenceWithValueMap.entrySet().stream()
                .filter(this::isReferenceNotValid)
                .map(this::createMessage)
                .collect(Collectors.toList());
    }

    private Message createMessage(Map.Entry<Structure.Reference, String> entry) {
        return new Message(ERROR_CODE,
                structure.getAttribute(entry.getKey().getAttribute()).getName(),
                entry.getValue());

    }

    private Field createFieldFilter(Structure structure, Structure.Reference reference) {
        Structure.Attribute referenceAttribute = structure.getAttribute(reference.getReferenceAttribute());
        return field(referenceAttribute);
    }

    private boolean isReferenceNotValid(Map.Entry<Structure.Reference, String> entry) {
        if(excludeAttributes.contains(entry.getKey().getAttribute()) || entry.getValue() == null) {
            return false;
        }
        Structure.Reference reference = entry.getKey();
        String referenceValue = entry.getValue();
        Integer versionId = reference.getReferenceVersion();
        Structure referenceStructure = versionService.getStructure(versionId);
        Field fieldFilter = createFieldFilter(referenceStructure, reference);
        Object referenceValueCasted = castReferenceValue(fieldFilter, referenceValue);
        AttributeFilter attributeFilter = new AttributeFilter(reference.getReferenceAttribute(), referenceValueCasted,
                referenceStructure.getAttribute(reference.getReferenceAttribute()).getType(), SearchTypeEnum.EXACT);
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(Collections.singletonList(attributeFilter), null);
        Page<RowValue> pagedData = versionService.search(versionId, searchDataCriteria);
        return (pagedData == null || !pagedData.hasContent());
    }

}
