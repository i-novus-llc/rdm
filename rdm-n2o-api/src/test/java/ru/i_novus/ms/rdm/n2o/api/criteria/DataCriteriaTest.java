package ru.i_novus.ms.rdm.n2o.api.criteria;

import net.n2oapp.criteria.api.Criteria;
import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.n2o.api.BaseTest;

public class DataCriteriaTest extends BaseTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final int TEST_OPT_LOCK_VALUE = 10;

    @Test
    public void testClass() {

        Criteria superCriteria = new Criteria();

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

        result.setVersionId(TEST_REFBOOK_VERSION_ID);
        result.setOptLockValue(TEST_OPT_LOCK_VALUE);
        result.setLocaleCode("test");

        return result;
    }

    private DataCriteria cloneCriteria(DataCriteria criteria) {

        DataCriteria result = new DataCriteria(criteria.getVersionId(), criteria.getOptLockValue(),
                criteria.getFilter(), criteria.getHasDataConflict());

        result.setPage(criteria.getPage());
        result.setSize(criteria.getSize());
        result.setSortings(criteria.getSortings());

        result.setLocaleCode(criteria.getLocaleCode());

        return result;
    }

    private DataCriteria copyCriteria(DataCriteria criteria) {

        DataCriteria result = new DataCriteria();

        result.setPage(criteria.getPage());
        result.setSize(criteria.getSize());
        result.setSortings(criteria.getSortings());

        result.setVersionId(criteria.getVersionId());
        result.setOptLockValue(criteria.getOptLockValue());
        result.setLocaleCode(criteria.getLocaleCode());

        result.setFilter(criteria.getFilter());
        result.setHasDataConflict(criteria.getHasDataConflict());

        return result;
    }
}