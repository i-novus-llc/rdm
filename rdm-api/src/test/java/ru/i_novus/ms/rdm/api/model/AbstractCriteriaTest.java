package ru.i_novus.ms.rdm.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AbstractCriteriaTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testClass() {

        AbstractCriteria criteria = new AbstractCriteria();
        assertNotNull(criteria);
        assertSpecialEquals(criteria);

        AbstractCriteria sameCriteria = new AbstractCriteria(criteria.getPageNumber(), criteria.getPageSize());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);
    }

    @Test
    public void testPaging() {

        AbstractCriteria criteria = new AbstractCriteria();

        AbstractCriteria unpagedCriteria = new AbstractCriteria();
        unpagedCriteria.makeUnpaged();
        assertObjects(Assert::assertNotEquals, criteria, unpagedCriteria);

        AbstractCriteria pagedCriteria = new AbstractCriteria(1000, 1000);
        assertObjects(Assert::assertNotEquals, criteria, pagedCriteria);
        assertObjects(Assert::assertNotEquals, unpagedCriteria, pagedCriteria);
    }

    @Test
    public void testSorting() {

        AbstractCriteria criteria = new AbstractCriteria();

        Sort.Order idOrder = new Sort.Order(Sort.Direction.ASC, "id");
        List<Sort.Order> orders = singletonList(idOrder);

        AbstractCriteria sortedCriteria = new AbstractCriteria();
        sortedCriteria.setOrders(orders);
        assertEquals(orders, sortedCriteria.getOrders());
        assertObjects(Assert::assertNotEquals, criteria, sortedCriteria);
    }
}