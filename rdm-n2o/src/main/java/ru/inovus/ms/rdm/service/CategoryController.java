package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.criteria.CategoryCriteria;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.Category;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.time.OffsetDateTime;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Controller
public class CategoryController {

    public static final String CATEGORY_REFBOOK_CODE = "CAT";
    public static final String CATEGORY_NAME_FIELD_CODE = "name";

    @Autowired
    VersionService versionService;

    /**
     * Поиск списка категорий из справочника категорий (находится по коду)
     */
    public Page<Category> getCategories(CategoryCriteria categoryCriteria) {

        SearchDataCriteria criteria = toSearchDataCriteria(categoryCriteria);

        Page<RefBookRowValue> rowValues = versionService.search(CATEGORY_REFBOOK_CODE, OffsetDateTime.now(), criteria);

        return new RestPage<>(rowValues.getContent(), criteria, rowValues.getTotalElements())
                .map(CategoryController::toCategory);

    }

    private static SearchDataCriteria toSearchDataCriteria(CategoryCriteria categoryCriteria) {
        SearchDataCriteria criteria = new SearchDataCriteria();
        if (isNotBlank(categoryCriteria.getName())) {
            criteria.setAttributeFilter(singleton(singletonList(
                    new AttributeFilter(CATEGORY_NAME_FIELD_CODE, categoryCriteria.getName(), FieldType.STRING, SearchTypeEnum.LIKE))));
        }
        criteria.setPageNumber(categoryCriteria.getPage() - 1);
        criteria.setPageSize(categoryCriteria.getSize());
        return criteria;
    }

    public static Category toCategory(RowValue rowValue) {
        return new Category(
                ofNullable(rowValue.getFieldValue("code"))
                        .map(FieldValue::getValue)
                        .map(String::valueOf).orElse(null),
                ofNullable(rowValue.getFieldValue(CATEGORY_NAME_FIELD_CODE))
                        .map(FieldValue::getValue)
                        .map(String::valueOf).orElse(null));
    }
}