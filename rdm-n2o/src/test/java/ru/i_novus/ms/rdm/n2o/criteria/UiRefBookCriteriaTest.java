package ru.i_novus.ms.rdm.n2o.criteria;

import org.junit.Test;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.n2o.model.RefBookStatus;

import static org.junit.Assert.*;

public class UiRefBookCriteriaTest {

    private static final Integer REFBOOK_ID = -10;

    @Test
    public void testSetRefBookId() {

        UiRefBookCriteria criteria = new UiRefBookCriteria();
        criteria.setRefBookId(REFBOOK_ID);

        assertFalse(CollectionUtils.isEmpty(criteria.getRefBookIds()));
        assertEquals(REFBOOK_ID, criteria.getRefBookIds().get(0));
    }

    @Test
    public void testSetStatusArchived() {

        UiRefBookCriteria criteria = new UiRefBookCriteria();
        criteria.setStatus(RefBookStatus.ARCHIVED);
        assertTrue(criteria.getIsArchived());
        assertFalse(criteria.getHasDraft());
        assertFalse(criteria.getHasPublished());
    }

    @Test
    public void testSetStatusHasDraft() {

        UiRefBookCriteria criteria = new UiRefBookCriteria();
        criteria.setStatus(RefBookStatus.HAS_DRAFT);
        assertTrue(criteria.getHasDraft());
        assertFalse(criteria.getIsArchived());
        assertFalse(criteria.getHasPublished());
    }

    @Test
    public void testSetStatusPublished() {

        UiRefBookCriteria criteria = new UiRefBookCriteria();
        criteria.setStatus(RefBookStatus.PUBLISHED);
        assertTrue(criteria.getHasPublished());
        assertFalse(criteria.getIsArchived());
        assertFalse(criteria.getHasDraft());
    }
}