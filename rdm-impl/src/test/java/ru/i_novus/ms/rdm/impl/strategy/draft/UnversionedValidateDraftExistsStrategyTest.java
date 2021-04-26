package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedValidateDraftExistsStrategyTest {

    @InjectMocks
    private UnversionedValidateDraftExistsStrategy strategy;

    @Test
    public void testValidate() {

        RefBookVersionEntity entity = createVersionEntity();

        testValidate(entity);

        entity.setStatus(RefBookVersionStatus.DRAFT);
        testValidate(entity);

        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        testValidate(entity);
    }

    @Test
    public void validateWhenNull() {

        try {
            strategy.validate(null);
            fail("Validate null");

        } catch (Exception e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("draft.not.found", e.getMessage());
        }
    }

    private void testValidate(RefBookVersionEntity entity) {
        try {
            strategy.validate(entity);

        } catch (Exception e) {
            fail("Unexpected exception throws");
        }
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setType(RefBookType.UNVERSIONED);

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setRefBook(refBookEntity);

        return entity;
    }
}