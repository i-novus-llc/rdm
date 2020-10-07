package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.criteria.CategoryCriteria;
import ru.i_novus.ms.rdm.n2o.model.Category;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Controller
@SuppressWarnings({"rawtypes", "java:S3740"})
public class CategoryController {

    private static final String CATEGORY_REFBOOK_CODE = "CAT";
    private static final String CATEGORY_NAME_FIELD_CODE = "name";

    @Autowired
    VersionRestService versionService;

    /**
     * Поиск списка категорий из справочника категорий (находится по коду)
     */
    @SuppressWarnings("unused")
    public Page<Category> getCategories(CategoryCriteria categoryCriteria) {

        SearchDataCriteria criteria = toSearchDataCriteria(categoryCriteria);

        Page<RefBookRowValue> rowValues = versionService.search(CATEGORY_REFBOOK_CODE, criteria);

        return new RestPage<>(rowValues.getContent(), criteria, rowValues.getTotalElements())
                .map(CategoryController::toCategory);

    }

    private static SearchDataCriteria toSearchDataCriteria(CategoryCriteria categoryCriteria) {

        SearchDataCriteria criteria = new SearchDataCriteria(categoryCriteria.getPage() - 1, categoryCriteria.getSize());

        if (isNotBlank(categoryCriteria.getName())) {
            AttributeFilter filter = new AttributeFilter(CATEGORY_NAME_FIELD_CODE,
                    categoryCriteria.getName(), FieldType.STRING, SearchTypeEnum.LIKE);
            criteria.addAttributeFilterList(singletonList(filter));
        }

        return criteria;
    }

    private static Category toCategory(RowValue rowValue) {
        return new Category(
                ofNullable(rowValue.getFieldValue("code"))
                        .map(FieldValue::getValue)
                        .map(String::valueOf).orElse(null),
                ofNullable(rowValue.getFieldValue(CATEGORY_NAME_FIELD_CODE))
                        .map(FieldValue::getValue)
                        .map(String::valueOf).orElse(null));
    }
}