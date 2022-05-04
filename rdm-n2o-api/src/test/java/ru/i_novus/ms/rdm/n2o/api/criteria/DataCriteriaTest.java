package ru.i_novus.ms.rdm.n2o.api.criteria;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.n2o.api.BaseTest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataCriteriaTest extends BaseTest {

    private static final int REFBOOK_VERSION_ID = -10;
    private static final int OPT_LOCK_VALUE = 10;

    @Test
    public void testEmpty() {

        DataRecordCriteria superCriteria = new DataRecordCriteria();

        DataCriteria emptyCriteria = new DataCriteria();
        assertSpecialEquals(emptyCriteria);
        assertObjects(Assert::assertNotEquals, superCriteria, emptyCriteria);
    }

    @Test
    public void testSameEmpty() {

        DataCriteria criteria = new DataCriteria();

        DataCriteria sameCriteria = new DataCriteria(criteria.getPageNumber(), criteria.getPageSize());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);

        sameCriteria = new DataCriteria(criteria.getPageNumber(), criteria.getPageSize(), criteria.getSort());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);
    }

    @Test
    public void testClass() {

        DataCriteria criteria = createCriteria();

        DataCriteria sameCriteria = createCriteria();
        assertObjects(Assert::assertEquals, criteria, sameCriteria);

        DataCriteria cloneCriteria = new DataCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, cloneCriteria);

        cloneCriteria = cloneCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, cloneCriteria);

        DataCriteria copyCriteria = copyCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, copyCriteria);
    }

    @Test
    public void testHasDataConflict() {

        // without localeCode:
        // -- null
        DataCriteria criteria = new DataCriteria();
        assertFalse(criteria.isHasDataConflict());

        // -- false
        criteria.setHasDataConflict(Boolean.FALSE);
        assertFalse(criteria.isHasDataConflict());

        // with localeCode:
        // -- false
        criteria.setLocaleCode("test");
        assertFalse(criteria.isHasDataConflict());

        // -- true
        criteria.setHasDataConflict(Boolean.TRUE);
        assertFalse(criteria.isHasDataConflict());

        // without localeCode:
        // -- true
        criteria.setLocaleCode(null);
        assertTrue(criteria.isHasDataConflict());
    }

    private DataCriteria createCriteria() {

        DataCriteria result = new DataCriteria();

        result.setVersionId(REFBOOK_VERSION_ID);
        result.setOptLockValue(OPT_LOCK_VALUE);
        result.setLocaleCode("test");
        
        result.setFilter(createFilter());
        result.setHasDataConflict(Boolean.FALSE);

        return result;
    }

    private Map<String, Serializable> createFilter() {

        Map<String, Serializable> result = new HashMap<>(2);
        result.put("field1", "value1");
        result.put("field2", "value2");

        return result;
    }

    private DataCriteria cloneCriteria(DataCriteria criteria) {

        DataCriteria result = new DataCriteria(criteria.getPageNumber(), criteria.getPageSize(), criteria.getSort());

        result.setVersionId(criteria.getVersionId());
        result.setOptLockValue(criteria.getOptLockValue());
        result.setLocaleCode(criteria.getLocaleCode());

        result.setFilter(criteria.getFilter());
        result.setHasDataConflict(criteria.getHasDataConflict());

        return result;
    }

    private DataCriteria copyCriteria(DataCriteria criteria) {

        DataCriteria result = new DataCriteria();

        result.setPageNumber(criteria.getPageNumber());
        result.setPageSize(criteria.getPageSize());
        result.setOrders(criteria.getOrders());

        result.setVersionId(criteria.getVersionId());
        result.setOptLockValue(criteria.getOptLockValue());
        result.setLocaleCode(criteria.getLocaleCode());

        result.setFilter(criteria.getFilter());
        result.setHasDataConflict(criteria.getHasDataConflict());

        return result;
    }
}