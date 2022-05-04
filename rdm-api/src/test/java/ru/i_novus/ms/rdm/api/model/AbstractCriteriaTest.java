package ru.i_novus.ms.rdm.api.model;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.BaseTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.model.AbstractCriteria.isEmptyOrders;

public class AbstractCriteriaTest extends BaseTest {

    @Test
    public void testEmptyClass() {

        AbstractCriteria emptyCriteria = new AbstractCriteria();
        assertSpecialEquals(emptyCriteria);
    }

    @Test
    public void testSameEmpty() {

        AbstractCriteria criteria = new AbstractCriteria();

        AbstractCriteria sameCriteria = new AbstractCriteria(criteria.getPageNumber(), criteria.getPageSize());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);

        sameCriteria = new AbstractCriteria(criteria.getPageNumber(), criteria.getPageSize(), criteria.getSort());
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

    @Test
    public void testIsEmptyOrders() {

        assertTrue(isEmptyOrders(emptyList(), emptyList()));

        List<Sort.Order> sortOrders = createSortOrders();
        assertEquals(2, sortOrders.size());

        assertFalse(isEmptyOrders(emptyList(), sortOrders));
        assertFalse(isEmptyOrders(sortOrders, emptyList()));

        assertTrue(isEmptyOrders(sortOrders, sortOrders));

        List<Sort.Order> reversedOrders = List.of(sortOrders.get(1), sortOrders.get(0));
        assertTrue(isEmptyOrders(reversedOrders, sortOrders));
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
        Sort.Order nameOrder = new Sort.Order(Sort.Direction.DESC, "name");

        return List.of(idOrder, nameOrder);
    }

    private AbstractCriteria createSortedCriteria(List<Sort.Order> orders) {

        AbstractCriteria criteria = new AbstractCriteria();
        criteria.setOrders(orders);

        return criteria;
    }
}