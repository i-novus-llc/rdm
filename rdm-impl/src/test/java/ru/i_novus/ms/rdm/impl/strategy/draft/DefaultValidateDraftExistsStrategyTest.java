package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.UnversionedRefBookEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class DefaultValidateDraftExistsStrategyTest {

    @InjectMocks
    private DefaultValidateDraftExistsStrategy strategy;

    @Test
    public void validateWhenNull() {

        try {
            strategy.validate(null, 0);
            fail("Validate null");

        } catch (Exception e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("draft.not.found", e.getMessage());
        }
    }

    @Test
    public void testValidateDraftWhenDefault() {

        RefBookVersionEntity entity = new DefaultRefBookEntity().createChangeableVersion();
        try {
            strategy.validate(entity, entity.getId());

        } catch (Exception e) {
            fail("Unexpected exception throws");
        }
    }

    @Test
    public void testValidateVersionWhenDefault() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setRefBook(new DefaultRefBookEntity());
        entity.setStatus(RefBookVersionStatus.PUBLISHED);

        try {
            strategy.validate(entity, entity.getId());
            fail(String.format("Exception expected with message: %s", "draft.not.found"));

        } catch (Exception e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("draft.not.found", e.getMessage());
        }
    }

    @Test
    public void testValidateDraftWhenUnversioned() {

        RefBookVersionEntity entity = new UnversionedRefBookEntity().createChangeableVersion();
        try {
            strategy.validate(entity, entity.getId());

        } catch (Exception e) {
            fail("Unexpected exception throws");
        }
    }
}