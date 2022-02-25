package ru.i_novus.ms.rdm.n2o.api.criteria;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.n2o.api.BaseTest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DataCriteriaTest extends BaseTest {

    private static final int REFBOOK_VERSION_ID = -10;
    private static final int OPT_LOCK_VALUE = 10;

    @Test
    public void testClass() {

        DataRecordCriteria superCriteria = new DataRecordCriteria();

        DataCriteria emptyCriteria = new DataCriteria();
        assertObjects(Assert::assertNotEquals, superCriteria, emptyCriteria);

        DataCriteria newCriteria = createCriteria();
        assertObjects(Assert::assertNotEquals, superCriteria, emptyCriteria);

        DataCriteria sameCriteria = createCriteria();
        assertObjects(Assert::assertEquals, newCriteria, sameCriteria);

        DataCriteria cloneCriteria = new DataCriteria(newCriteria);
        assertObjects(Assert::assertEquals, newCriteria, cloneCriteria);

        cloneCriteria = cloneCriteria(newCriteria);
        assertObjects(Assert::assertEquals, newCriteria, cloneCriteria);

        DataCriteria copyCriteria = copyCriteria(newCriteria);
        assertObjects(Assert::assertEquals, newCriteria, copyCriteria);
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