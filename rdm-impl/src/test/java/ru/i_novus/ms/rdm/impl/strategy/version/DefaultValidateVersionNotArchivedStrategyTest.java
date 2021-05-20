package ru.i_novus.ms.rdm.impl.strategy.version;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class DefaultValidateVersionNotArchivedStrategyTest {

    @InjectMocks
    private DefaultValidateVersionNotArchivedStrategy strategy;

    @Test
    public void testValidate() {

        RefBookVersionEntity entity = createVersionEntity();

        try {
            strategy.validate(entity);

        } catch (Exception e) {
            fail("Unexpected exception throws");
        }
    }

    @Test
    public void testValidateArchived() {

        RefBookVersionEntity entity = createVersionEntity();
        entity.getRefBook().setArchived(Boolean.TRUE);

        try {
            strategy.validate(entity);
            fail(String.format("Exception expected with message: %s", "refbook.with.code.is.archived"));

        } catch (Exception e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("refbook.with.code.is.archived", e.getMessage());
        }
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookEntity refBookEntity = new DefaultRefBookEntity();

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setRefBook(refBookEntity);

        return entity;
    }
}