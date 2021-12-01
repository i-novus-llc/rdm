package ru.i_novus.ms.rdm.api.model.refdata;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

public class SearchDataCriteriaTest extends BaseTest {

    private static final SearchDataCriteria EMPTY_SEARCH_DATA_CRITERIA = new SearchDataCriteria(0, 1);

    @Test
    public void testClass() {

        SearchDataCriteria emptyCriteria = new SearchDataCriteria();
        assertObjects(Assert::assertNotEquals, EMPTY_SEARCH_DATA_CRITERIA, emptyCriteria);

        SearchDataCriteria sameCriteria = new SearchDataCriteria(emptyCriteria.getPageNumber(), emptyCriteria.getPageSize());
        assertObjects(Assert::assertEquals, emptyCriteria, sameCriteria);

        SearchDataCriteria newCriteria = createCriteria();
        assertObjects(Assert::assertNotEquals, emptyCriteria, newCriteria);

        sameCriteria = createCriteria();
        assertObjects(Assert::assertEquals, newCriteria, sameCriteria);

        SearchDataCriteria copyCriteria = copyCriteria(newCriteria);
        assertObjects(Assert::assertEquals, newCriteria, copyCriteria);

        AbstractCriteria superCriteria = new AbstractCriteria();
        assertObjects(Assert::assertNotEquals, emptyCriteria, superCriteria);

        superCriteria = new AbstractCriteria(emptyCriteria.getPageNumber(), emptyCriteria.getPageSize());
        assertObjects(Assert::assertNotEquals, emptyCriteria, superCriteria);
    }

    private SearchDataCriteria createCriteria() {

        SearchDataCriteria result = new SearchDataCriteria();

        result.setLocaleCode("test");
        result.setCommonFilter("common-filter");
        result.setRowHashList(List.of("hash1", "hash2"));
        result.setRowSystemIds(List.of(1L, 2L));

        return result;
    }

    private SearchDataCriteria copyCriteria(SearchDataCriteria criteria) {

        SearchDataCriteria result = new SearchDataCriteria();

        result.setPageNumber(criteria.getPageNumber());
        result.setPageSize(criteria.getPageSize());

        if (!isEmpty(criteria.getOrders())) {
            result.setOrders(criteria.getOrders());
        }

        result.setLocaleCode(criteria.getLocaleCode());
        result.setAttributeFilters(criteria.getAttributeFilters());
        result.setPlainAttributeFilters(criteria.getPlainAttributeFilters());

        result.setCommonFilter(criteria.getCommonFilter());
        result.setRowHashList(criteria.getRowHashList());
        result.setRowSystemIds(criteria.getRowSystemIds());

        return result;
    }
}