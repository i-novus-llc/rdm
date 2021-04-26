package ru.i_novus.ms.rdm.impl.strategy.version;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class DefaultValidateVersionExistsStrategyTest {

    @InjectMocks
    private DefaultValidateVersionExistsStrategy strategy;

    @Test
    public void validate() {

        RefBookVersionEntity entity = new RefBookVersionEntity();

        try {
            strategy.validate(entity);

        } catch (Exception e) {
            fail("Unexpected exception throws");
        }
    }

    @Test
    public void validateWhenNull() {

        try {
            strategy.validate(null);
            fail("Validate null");

        } catch (Exception e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("version.not.found", e.getMessage());
        }
    }
}