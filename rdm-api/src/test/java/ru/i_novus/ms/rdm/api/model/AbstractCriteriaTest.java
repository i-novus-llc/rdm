package ru.i_novus.ms.rdm.api.model;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.BaseTest;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AbstractCriteriaTest extends BaseTest {

    @Test
    public void testClass() {

        AbstractCriteria criteria = new AbstractCriteria();
        assertNotNull(criteria);
        assertSpecialEquals(criteria);

        AbstractCriteria sameCriteria = new AbstractCriteria(criteria.getPageNumber(), criteria.getPageSize());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);
    }

    @Test
    public void testCloneEmpty() {

        testClone(new AbstractCriteria());
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

        List<Sort.Order> orders = createSortOrders();
        AbstractCriteria sortedCriteria = createSortedCriteria(orders);

        assertEquals(orders, sortedCriteria.getOrders());
        assertObjects(Assert::assertNotEquals, criteria, sortedCriteria);
    }

    @Test
    public void testCloneSorting() {

        List<Sort.Order> orders = createSortOrders();
        testClone(createSortedCriteria(orders));
    }

    private void testClone(AbstractCriteria criteria) {

        AbstractCriteria cloneCriteria = new AbstractCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, cloneCriteria);

        cloneCriteria = cloneCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, cloneCriteria);
    }

    private AbstractCriteria cloneCriteria(AbstractCriteria criteria) {

        return new AbstractCriteria(criteria.getPageNumber(), criteria.getPageSize(), criteria.getSort());
    }

    private List<Sort.Order> createSortOrders() {

        Sort.Order idOrder = new Sort.Order(Sort.Direction.ASC, "id");
        return singletonList(idOrder);
    }

    private AbstractCriteria createSortedCriteria(List<Sort.Order> orders) {

        AbstractCriteria criteria = new AbstractCriteria();
        criteria.setOrders(orders);

        return criteria;
    }
}