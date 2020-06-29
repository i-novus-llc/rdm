package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.n2o.criteria.ReferenceCriteria;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static ru.inovus.ms.rdm.api.util.FieldValueUtils.rowValueToDisplayValueWithPkLeading;

@Controller
public class ReferenceController {

    @Autowired
    private VersionService versionService;

    /**
     * Поиск списка значений справочника для ссылки.
     */
    @SuppressWarnings("unused") // used in: reference.query.xml, see DataRecordPageProvider
    public Page<Reference> getList(ReferenceCriteria referenceCriteria) {

        Structure.Reference reference = versionService
                .getStructure(referenceCriteria.getVersionId())
                .getReference(referenceCriteria.getReference());

        RefBookVersion referenceVersion = versionService.getLastPublishedVersion(reference.getReferenceCode());
        String pk = referenceVersion.getStructure().getPrimary().iterator().next().getCode();
        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(referenceVersion.getStructure());

        SearchDataCriteria criteria = toSearchDataCriteria(referenceAttribute, referenceCriteria);

        Page<RefBookRowValue> rowValues = versionService.search(reference.getReferenceCode(), criteria);

        return new RestPage<>(rowValues.getContent(), criteria, rowValues.getTotalElements())
                .map(rowValue -> toReferenceValue(referenceAttribute, reference.getDisplayExpression(), rowValue, pk));
    }

    private Reference toReferenceValue(Structure.Attribute attribute, String displayExpression, RowValue rowValue, String pk) {
        Reference referenceValue = new Reference();
        referenceValue.setValue(String.valueOf(rowValue.getFieldValue(attribute.getCode()).getValue()));
        referenceValue.setDisplayValue(rowValueToDisplayValueWithPkLeading(displayExpression, pk, rowValue));
        return referenceValue;
    }

    private SearchDataCriteria toSearchDataCriteria(Structure.Attribute attribute, ReferenceCriteria referenceCriteria) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        if (isNotBlank(referenceCriteria.getValue())) {
            criteria.setAttributeFilter(singleton(singletonList(
                    new AttributeFilter(attribute.getCode(), referenceCriteria.getValue(), FieldType.STRING, SearchTypeEnum.EXACT))));
        }

        if (isNotBlank(referenceCriteria.getDisplayValue()))
            criteria.setCommonFilter(referenceCriteria.getDisplayValue());

        criteria.setPageNumber(referenceCriteria.getPage() - 1);
        criteria.setPageSize(referenceCriteria.getSize());

        return criteria;
    }
}
