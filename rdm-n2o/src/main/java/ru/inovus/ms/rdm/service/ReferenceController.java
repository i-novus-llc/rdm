package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.criteria.ReferenceCriteria;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Controller
public class ReferenceController {

    @Autowired
    private VersionService versionService;


    /**
     * Поиск списка категорий из справочника категорий (находится по коду)
     */
    public Page<Reference> getList(ReferenceCriteria referenceCriteria) {

        Structure.Reference reference = versionService
                .getStructure(referenceCriteria.getVersionId())
                .getReference(referenceCriteria.getReference());

        SearchDataCriteria criteria = toSearchDataCriteria(reference, referenceCriteria);

        Page<RefBookRowValue> rowValues = versionService.search(reference.getReferenceVersion(), criteria);


        return new RestPage<>(rowValues.getContent(), criteria, rowValues.getTotalElements())
                .map(rowValue -> toReference(reference, rowValue));
    }

    private static Reference toReference(Structure.Reference referenceAttribute, RowValue rowValue) {
        Reference referenceValue = new Reference();
        referenceValue.setValue(String.valueOf(rowValue.getFieldValue(referenceAttribute.getReferenceAttribute()).getValue()));
        Map<String, Object> map = new HashMap<>();
        ((LongRowValue)rowValue).getFieldValues().forEach(fieldValue -> map.put(fieldValue.getField(), fieldValue.getValue()));
        referenceValue.setDisplayValue(new StrSubstitutor(map).replace(referenceAttribute.getDisplayExpression()));
        return referenceValue;
    }

    private SearchDataCriteria toSearchDataCriteria(Structure.Reference reference, ReferenceCriteria referenceCriteria) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        if (isNotBlank(referenceCriteria.getValue())) {
            criteria.setAttributeFilter(singleton(singletonList(
                    new AttributeFilter(reference.getReferenceAttribute(), referenceCriteria.getValue(), FieldType.STRING, SearchTypeEnum.EXACT))));
        }
        if (isNotBlank(referenceCriteria.getDisplayValue()))
            criteria.setCommonFilter(referenceCriteria.getDisplayValue());
        criteria.setPageNumber(referenceCriteria.getPage() - 1);
        criteria.setPageSize(referenceCriteria.getSize());
        return criteria;
    }
}
