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

    @Autowired
    private VersionRestService versionService;

    /**
     * Поиск списка значений справочника для ссылки.
     */
    @SuppressWarnings("unused") // used in: reference.query.xml, see DataRecordPageProvider
    public Page<Reference> getList(ReferenceCriteria referenceCriteria) {

        Structure.Reference reference = versionService
                .getStructure(referenceCriteria.getVersionId())
                .getReference(referenceCriteria.getReference());

        RefBookVersion referenceVersion = versionService.getLastPublishedVersion(reference.getReferenceCode());
        if (referenceVersion == null || referenceVersion.hasEmptyStructure()) {
            return new RestPage<>();
        }

        Structure referenceStructure = referenceVersion.getStructure();
        if (!referenceStructure.hasPrimary()
                || hasAbsentPlaceholder(reference.getDisplayExpression(), referenceStructure)) {
            return new RestPage<>();
        }

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(referenceStructure);
        SearchDataCriteria criteria = toSearchDataCriteria(referenceAttribute, referenceCriteria);
        Page<RefBookRowValue> rowValues = versionService.search(reference.getReferenceCode(), criteria);

        List<String> primaryKeyCodes = referenceStructure.getPrimaryCodes();

        return new RestPage<>(rowValues.getContent(), criteria, rowValues.getTotalElements())
                .map(rowValue -> toReferenceValue(referenceAttribute, reference.getDisplayExpression(), rowValue, primaryKeyCodes));
    }

    private SearchDataCriteria toSearchDataCriteria(Structure.Attribute attribute, ReferenceCriteria referenceCriteria) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        if (isNotBlank(referenceCriteria.getValue())) {

            AttributeFilter filter = new AttributeFilter(attribute.getCode(),
                    referenceCriteria.getValue(), FieldType.STRING, SearchTypeEnum.EXACT);
            criteria.addAttributeFilterList(singletonList(filter));
        }

        if (isNotBlank(referenceCriteria.getDisplayValue()))
            criteria.setCommonFilter(referenceCriteria.getDisplayValue());

        criteria.setPageNumber(referenceCriteria.getPage() - 1);
        criteria.setPageSize(referenceCriteria.getSize());

        return criteria;
    }

    /** Вычисление отображаемого значения ссылки. */
    private Reference toReferenceValue(Structure.Attribute attribute, String displayExpression,
                                       RowValue rowValue, List<String> primaryKeyCodes) {
        Reference referenceValue = new Reference();
        referenceValue.setValue(String.valueOf(rowValue.getFieldValue(attribute.getCode()).getValue()));
        referenceValue.setDisplayValue(toDisplayValue(displayExpression, rowValue, primaryKeyCodes));
        return referenceValue;
    }
}
