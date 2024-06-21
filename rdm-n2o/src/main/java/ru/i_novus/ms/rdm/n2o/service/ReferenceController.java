package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.criteria.ReferenceCriteria;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.i_novus.ms.rdm.api.util.FieldValueUtils.toDisplayValue;
import static ru.i_novus.ms.rdm.api.util.StructureUtils.hasAbsentPlaceholder;

@Controller
@SuppressWarnings({"rawtypes", "java:S3740"})
public class ReferenceController {

    private final VersionRestService versionService;

    @Autowired
    public ReferenceController(VersionRestService versionService) {

        this.versionService = versionService;
    }

    /**
     * Поиск списка значений справочника для ссылки.
     */
    @SuppressWarnings("unused") // used in: reference.query.xml, see DataRecordPageProvider
    public Page<Reference> getList(ReferenceCriteria criteria) {

        Structure.Reference reference = versionService
                .getStructure(criteria.getVersionId())
                .getReference(criteria.getReference());

        // Версия справочника, на который ссылаются
        RefBookVersion referenceVersion = versionService.getLastPublishedVersion(reference.getReferenceCode());
        if (referenceVersion == null || referenceVersion.hasEmptyStructure()) {
            return new RestPage<>();
        }

        Structure referenceStructure = referenceVersion.getStructure();
        if (!referenceStructure.hasPrimary()
                || hasAbsentPlaceholder(reference.getDisplayExpression(), referenceStructure)) {
            return new RestPage<>();
        }

        // Атрибут, на который ссылаются
        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(referenceStructure);
        SearchDataCriteria searchDataCriteria = toSearchDataCriteria(referenceAttribute, criteria);
        Page<RefBookRowValue> rowValues = versionService.search(reference.getReferenceCode(), searchDataCriteria);

        List<String> primaryCodes = referenceStructure.getPrimaryCodes();

        return new RestPage<>(rowValues.getContent(), searchDataCriteria, rowValues.getTotalElements())
                .map(rowValue ->
                        toReferenceValue(referenceAttribute, reference.getDisplayExpression(), rowValue, primaryCodes)
                );
    }

    private SearchDataCriteria toSearchDataCriteria(Structure.Attribute attribute,
                                                    ReferenceCriteria criteria) {

        SearchDataCriteria result = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize());

        if (isNotBlank(criteria.getValue())) {
            AttributeFilter filter = new AttributeFilter(attribute.getCode(),
                    criteria.getValue(), FieldType.STRING, SearchTypeEnum.EXACT);
            result.addAttributeFilterList(singletonList(filter));
        }

        if (isNotBlank(criteria.getDisplayValue())) {
            result.setCommonFilter(criteria.getDisplayValue());
        }

        return result;
    }

    /** Вычисление отображаемого значения ссылки. */
    private Reference toReferenceValue(Structure.Attribute attribute,
                                       String displayExpression,
                                       RowValue rowValue,
                                       List<String> primaryKeyCodes) {

        Reference referenceValue = new Reference();
        referenceValue.setValue(String.valueOf(rowValue.getFieldValue(attribute.getCode()).getValue()));
        referenceValue.setDisplayValue(toDisplayValue(displayExpression, rowValue, primaryKeyCodes));

        return referenceValue;
    }
}
