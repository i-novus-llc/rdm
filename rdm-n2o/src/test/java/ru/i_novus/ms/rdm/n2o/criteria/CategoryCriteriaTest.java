package ru.i_novus.ms.rdm.n2o.criteria;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.n2o.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CategoryCriteriaTest extends BaseTest {

    private static final String CATEGORY_VALUE = "value";

    @Test
    public void testEmpty() {

        AbstractCriteria superCriteria = new AbstractCriteria();

        CategoryCriteria emptyCriteria = new CategoryCriteria();
        assertSpecialEquals(emptyCriteria);
        assertObjects(Assert::assertNotEquals, superCriteria, emptyCriteria);
    }

    @Test
    public void testClass() {

        CategoryCriteria criteria = new CategoryCriteria();
        assertNull(criteria.getName());

        criteria.setName(CATEGORY_VALUE);
        assertEquals(CATEGORY_VALUE, criteria.getName());
    }
}