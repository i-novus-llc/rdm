package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.criteria.api.SortingDirection;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;

import java.lang.reflect.InvocationTargetException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class RestCriteriaConstructorTest {

    private static final String SORTED_FIELD = "id";

    @Test
    public void testWhenEmpty() {

        RestCriteriaConstructor constructor = new RestCriteriaConstructor(emptyList());

        N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        Criteria result = construct(constructor, criteria, Criteria.class);
        assertNotEquals(criteria.getPage(), result.getPage());
        assertNotEquals(criteria.getSize(), result.getSize());
        assertNull(result.getSorting());
    }

    @Test
    public void testCriteria() {

        CriteriaConstructResolver testResolver = new N2oCriteriaConstructResolver();
        RestCriteriaConstructor constructor = new RestCriteriaConstructor(singletonList(testResolver));

        N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        Criteria result = construct(constructor, criteria, Criteria.class);
        assertCriteriaEquals(criteria, result);
    }

    private void assertCriteriaEquals(Criteria expected, Criteria actual) {

        assertNotNull(actual);
        assertEquals(expected.getPage(), actual.getPage());
        assertEquals(expected.getSize(), actual.getSize());

        assertSortingEquals(expected.getSorting(), actual.getSorting());
    }

    private void assertSortingEquals(Sorting expected, Sorting actual) {

        assertNotNull(actual);
        assertEquals(expected.getField(), actual.getField());
        assertEquals(expected.getDirection(), actual.getDirection());
    }

    @Test
    public void testRestCriteria() {

        RestCriteriaConstructResolver testResolver = new RestCriteriaConstructResolver();
        RestCriteriaConstructor constructor = new RestCriteriaConstructor(singletonList(testResolver));

        N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        RestCriteria result = construct(constructor, criteria, VersionCriteria.class);
        assertCriteriaEquals(criteria, result);
    }

    @Test
    public void testRefBookCriteria() {

        RefBookCriteriaConstructResolver testResolver = new RefBookCriteriaConstructResolver();
        RestCriteriaConstructor constructor = new RestCriteriaConstructor(singletonList(testResolver));

        N2oPreparedCriteria criteria = createN2oPreparedCriteria();
        criteria.getSorting().setField("name");

        RefBookCriteria result = construct(constructor, criteria, RefBookCriteria.class);
        assertNotNull(result);
        assertNotNull(result.getSort());

        final String passportName = "passport.name";
        Sort.Order sortOrder = result.getSort().getOrderFor(passportName);
        assertNotNull(sortOrder);

        criteria.getSorting().setField(SORTED_FIELD);
        result.setOrders(singletonList(new Sort.Order(Sort.Direction.ASC, SORTED_FIELD)));

        assertCriteriaEquals(criteria, result);
    }

    private void assertCriteriaEquals(Criteria expected, RestCriteria actual) {

        assertNotNull(actual);
        assertEquals(expected.getPage() - 1, actual.getPageNumber());
        assertEquals(expected.getSize(), actual.getPageSize());

        assertNotNull(actual.getSort());
        assertSortingEquals(expected.getSorting(), actual.getSort().getOrderFor(SORTED_FIELD));
    }

    private void assertSortingEquals(Sorting expected, Sort.Order actual) {

        assertNotNull(actual);
        assertEquals(expected.getField(), actual.getProperty());
        assertEquals(expected.getDirection().toString(), actual.getDirection().toString());
    }

    private N2oPreparedCriteria createN2oPreparedCriteria() {

        N2oPreparedCriteria criteria = new N2oPreparedCriteria();
        criteria.setPage(11);
        criteria.setSize(111);
        criteria.addSorting(new Sorting(SORTED_FIELD, SortingDirection.ASC));

        return criteria;
    }

    @SuppressWarnings("unchecked")
    private <T> T construct(RestCriteriaConstructor constructor,
                            N2oPreparedCriteria criteria,
                            Class<T> criteriaClass) {

        T instance = newInstance(criteriaClass);
        assertTrue(criteriaClass.isInstance(instance));

        Object result = constructor.construct(criteria, instance);
        assertNotNull(result);
        assertTrue(criteriaClass.isInstance(result));

        return (T) result; // unchecked
    }

    private <T> T newInstance(Class<T> criteriaClass) {
        try {
            return criteriaClass.getDeclaredConstructor().newInstance();

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }
}