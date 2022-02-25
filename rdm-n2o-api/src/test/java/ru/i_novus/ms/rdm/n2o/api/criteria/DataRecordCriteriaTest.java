package ru.i_novus.ms.rdm.n2o.api.criteria;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.n2o.api.BaseTest;

public class DataRecordCriteriaTest extends BaseTest {

    private static final long RECORD_ID = -100;
    private static final int REFBOOK_VERSION_ID = -10;
    private static final int OPT_LOCK_VALUE = 10;

    @Test
    public void testEmpty() {

        AbstractCriteria superCriteria = new AbstractCriteria();

        DataRecordCriteria emptyCriteria = new DataRecordCriteria();
        assertSpecialEquals(emptyCriteria);
        assertObjects(Assert::assertNotEquals, superCriteria, emptyCriteria);
    }

    @Test
    public void testSameEmpty() {

        DataRecordCriteria criteria = new DataRecordCriteria();

        DataRecordCriteria sameCriteria = new DataRecordCriteria(criteria.getPageNumber(), criteria.getPageSize());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);

        sameCriteria = new DataRecordCriteria(criteria.getPageNumber(), criteria.getPageSize(), criteria.getSort());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);
    }

    @Test
    public void testClass() {

        DataRecordCriteria criteria = createCriteria();

        DataRecordCriteria sameCriteria = createCriteria();
        assertObjects(Assert::assertEquals, criteria, sameCriteria);

        DataRecordCriteria cloneCriteria = new DataRecordCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, cloneCriteria);

        cloneCriteria = cloneCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, cloneCriteria);

        DataRecordCriteria copyCriteria = copyCriteria(criteria);
        assertObjects(Assert::assertEquals, criteria, copyCriteria);
    }

    private DataRecordCriteria createCriteria() {

        DataRecordCriteria result = new DataRecordCriteria();

        result.setId(RECORD_ID);
        result.setVersionId(REFBOOK_VERSION_ID);
        result.setOptLockValue(OPT_LOCK_VALUE);
        result.setLocaleCode("test");
        result.setDataAction("dataAction");

        return result;
    }

    private DataRecordCriteria cloneCriteria(DataRecordCriteria criteria) {

        DataRecordCriteria result = new DataRecordCriteria(criteria.getPageNumber(), criteria.getPageSize(), criteria.getSort());

        result.setId(criteria.getId());
        result.setVersionId(criteria.getVersionId());
        result.setOptLockValue(criteria.getOptLockValue());
        result.setLocaleCode(criteria.getLocaleCode());
        result.setDataAction(criteria.getDataAction());

        return result;
    }

    private DataRecordCriteria copyCriteria(DataRecordCriteria criteria) {

        DataRecordCriteria result = new DataRecordCriteria();

        result.setPageNumber(criteria.getPageNumber());
        result.setPageSize(criteria.getPageSize());
        result.setOrders(criteria.getOrders());

        result.setId(criteria.getId());
        result.setVersionId(criteria.getVersionId());
        result.setOptLockValue(criteria.getOptLockValue());
        result.setLocaleCode(criteria.getLocaleCode());
        result.setDataAction(criteria.getDataAction());

        return result;
    }
}