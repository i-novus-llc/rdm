package ru.i_novus.ms.rdm.n2o.criteria;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CategoryCriteriaTest {

    private static final String CATEGORY_VALUE = "value";

    @Test
    public void testClass() {

        CategoryCriteria criteria = new CategoryCriteria();
        assertNull(criteria.getName());

        criteria.setName(CATEGORY_VALUE);
        assertEquals(CATEGORY_VALUE, criteria.getName());
    }
}