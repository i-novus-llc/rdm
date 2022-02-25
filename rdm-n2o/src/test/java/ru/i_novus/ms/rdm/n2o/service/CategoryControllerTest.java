package ru.i_novus.ms.rdm.n2o.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.criteria.CategoryCriteria;
import ru.i_novus.ms.rdm.n2o.model.Category;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CategoryControllerTest {

    private static final String CATEGORY_REFBOOK_CODE = "CAT";
    private static final int CATEGORY_REFBOOK_VERSION_ID = -10;
    private static final String CATEGORY_FILTER_VALUE = "name_1";

    @InjectMocks
    private CategoryController controller;

    @Mock
    private VersionRestService versionService;

    @Test
    public void testGetAllCategories() {

        CategoryCriteria criteria = new CategoryCriteria();

        SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);

        List<RefBookRowValue> rowValues = createContent();
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(CATEGORY_REFBOOK_CODE), eq(searchDataCriteria))).thenReturn(rowValuesPage);

        Page<Category> categories = controller.getCategories(criteria);
        assertNotNull(categories);
        assertNotNull(categories.getContent());
        assertEquals(rowValues.size(), categories.getContent().size());
    }

    @Test
    public void testGetCategoriesByName() {

        CategoryCriteria criteria = new CategoryCriteria();
        criteria.setName(CATEGORY_FILTER_VALUE);

        SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);
        AttributeFilter filter = new AttributeFilter("name", CATEGORY_FILTER_VALUE, FieldType.STRING, SearchTypeEnum.LIKE);
        searchDataCriteria.addAttributeFilterList(singletonList(filter));

        List<RefBookRowValue> rowValues = createContent().subList(0, 1);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(CATEGORY_REFBOOK_CODE), eq(searchDataCriteria))).thenReturn(rowValuesPage);

        Page<Category> categories = controller.getCategories(criteria);
        assertNotNull(categories);
        assertNotNull(categories.getContent());
        assertEquals(rowValues.size(), categories.getContent().size());
    }

    private SearchDataCriteria createSearchDataCriteria(CategoryCriteria criteria) {

        return new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize());
    }

    private List<RefBookRowValue> createContent() {

        int rowValueCount = 5;

        List<RefBookRowValue> rowValues = new ArrayList<>(rowValueCount);

        LongStream.range(1, rowValueCount + 1).forEach(systemId ->
                rowValues.add(new RefBookRowValue(createLongRowValue(systemId), CATEGORY_REFBOOK_VERSION_ID))
        );

        return rowValues;
    }

    private LongRowValue createLongRowValue(long systemId) {

        return new LongRowValue(systemId, asList(
                new IntegerFieldValue("code", BigInteger.valueOf(systemId)),
                new StringFieldValue("name", "name_" + systemId)
        ));
    }
}