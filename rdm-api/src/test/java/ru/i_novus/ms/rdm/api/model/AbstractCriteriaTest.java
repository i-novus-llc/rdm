package ru.i_novus.ms.rdm.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import static org.junit.Assert.assertNotNull;
import static ru.i_novus.ms.rdm.api.util.RefBookTestUtils.assertObjects;

public class AbstractCriteriaTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testClass() {

        AbstractCriteria criteria = new AbstractCriteria();
        assertNotNull(criteria);

        AbstractCriteria sameCriteria = new AbstractCriteria(criteria.getPageNumber(), criteria.getPageSize());
        assertObjects(Assert::assertEquals, criteria, sameCriteria);

        AbstractCriteria unpagedCriteria = new AbstractCriteria();
        unpagedCriteria.makeUnpaged();
        assertObjects(Assert::assertNotEquals, criteria, unpagedCriteria);

        AbstractCriteria pagedCriteria = new AbstractCriteria(1000, 1000);
        assertObjects(Assert::assertNotEquals, criteria, pagedCriteria);
        assertObjects(Assert::assertNotEquals, unpagedCriteria, pagedCriteria);
    }
}