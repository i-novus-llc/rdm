package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedValidateDraftExistsStrategyTest {

    @InjectMocks
    private UnversionedValidateDraftExistsStrategy strategy;

    @Test
    public void testValidateUnversioned() {

        RefBookVersionEntity entity = createVersionEntity();

        testValidateUnversioned(entity);

        entity.setStatus(RefBookVersionStatus.DRAFT);
        testValidateUnversioned(entity);

        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        testValidateUnversioned(entity);
    }

    private void testValidateUnversioned(RefBookVersionEntity entity) {
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