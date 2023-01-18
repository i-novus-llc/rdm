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

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Controller
public class CategoryController {

    static final String CATEGORY_REFBOOK_CODE = "CAT";
    static final String CATEGORY_CODE_FIELD_CODE = "code";
    static final String CATEGORY_NAME_FIELD_CODE = "name"; // NB: this is id

    private final VersionRestService versionService;

    @Autowired
    public CategoryController(VersionRestService versionService) {

        this.versionService = versionService;
    }

    /**
     * Поиск списка категорий из справочника категорий (находится по коду).
     */
    @SuppressWarnings("unused")
    public Page<Category> getList(CategoryCriteria criteria) {

        SearchDataCriteria searchDataCriteria = toSearchDataCriteria(criteria);

        Page<RefBookRowValue> rowValues = versionService.search(CATEGORY_REFBOOK_CODE, searchDataCriteria);

        return new RestPage<>(rowValues.getContent(), searchDataCriteria, rowValues.getTotalElements())
                .map(CategoryController::toCategory);
    }

    private static SearchDataCriteria toSearchDataCriteria(CategoryCriteria criteria) {

        SearchDataCriteria result = new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize());

        if (isNotBlank(criteria.getName())) {
            AttributeFilter filter = new AttributeFilter(CATEGORY_NAME_FIELD_CODE,
                    criteria.getName(), FieldType.STRING, SearchTypeEnum.LIKE);
            result.addAttributeFilterList(singletonList(filter));
        }

        return result;
    }

    private static Category toCategory(RefBookRowValue rowValue) {

        return new Category(
                ofNullable(rowValue.getFieldValue(CATEGORY_CODE_FIELD_CODE))
                        .map(FieldValue::getValue)
                        .map(String::valueOf).orElse(null),
                ofNullable(rowValue.getFieldValue(CATEGORY_NAME_FIELD_CODE))
                        .map(FieldValue::getValue)
                        .map(String::valueOf).orElse(null)
        );
    }
}