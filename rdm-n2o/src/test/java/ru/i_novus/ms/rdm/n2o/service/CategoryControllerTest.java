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
import ru.i_novus.ms.rdm.n2o.criteria.CategoryCriteria;
import ru.i_novus.ms.rdm.n2o.model.Category;
import ru.i_novus.ms.rdm.rest.client.impl.VersionRestServiceRestClient;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.util.List;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.n2o.service.CategoryController.*;

@RunWith(MockitoJUnitRunner.class)
public class CategoryControllerTest {

    private static final int CATEGORY_REFBOOK_VERSION_ID = -10;
    private static final long CATEGORY_DATA_COUNT = 5L;

    @InjectMocks
    private CategoryController controller;

    @Mock
    private VersionRestServiceRestClient versionService;

    @Test
    public void testGetListAll() {

        final CategoryCriteria criteria = new CategoryCriteria();

        final SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);

        final List<RefBookRowValue> rowValues = createContent();
        final Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(CATEGORY_REFBOOK_CODE), eq(searchDataCriteria))).thenReturn(rowValuesPage);

        final Page<Category> categories = controller.getList(criteria);
        assertNotNull(categories);
        assertNotNull(categories.getContent());
        assertEquals(rowValues.size(), categories.getContent().size());
    }

    @Test
    public void testGetListByName() {

        final String categoryName = "some_name";

        final CategoryCriteria criteria = new CategoryCriteria();
        criteria.setName(categoryName);

        final SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);
        AttributeFilter filter = new AttributeFilter(CATEGORY_NAME_FIELD_CODE,
                categoryName, FieldType.STRING, SearchTypeEnum.LIKE);
        searchDataCriteria.addAttributeFilterList(singletonList(filter));

        final List<RefBookRowValue> rowValues = createContent().subList(0, 1);
        final Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(CATEGORY_REFBOOK_CODE), eq(searchDataCriteria))).thenReturn(rowValuesPage);

        final Page<Category> categories = controller.getList(criteria);
        assertNotNull(categories);
        assertNotNull(categories.getContent());
        assertEquals(rowValues.size(), categories.getContent().size());
    }

    private SearchDataCriteria createSearchDataCriteria(CategoryCriteria criteria) {

        return new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize());
    }

    private List<RefBookRowValue> createContent() {

        return LongStream.range(1, CATEGORY_DATA_COUNT + 1)
                .mapToObj(systemId -> new RefBookRowValue(createLongRowValue(systemId), CATEGORY_REFBOOK_VERSION_ID))
                .collect(toList());
    }

    private LongRowValue createLongRowValue(long systemId) {

        return new LongRowValue(systemId, asList(
                new StringFieldValue(CATEGORY_CODE_FIELD_CODE, "code_" + systemId),
                new StringFieldValue(CATEGORY_NAME_FIELD_CODE, "name_" + systemId)
        ));
    }
}