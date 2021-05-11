package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.util.UnversionedVersionNumberStrategy;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedCreateDraftEntityStrategyTest {

    @InjectMocks
    private UnversionedCreateDraftEntityStrategy strategy;

    @Mock
    private UnversionedVersionNumberStrategy versionNumberStrategy;

    @Test
    public void testCreate() {

        final String firstVersion = "-1.0";
        when(versionNumberStrategy.first()).thenReturn(firstVersion);

        RefBookEntity refBookEntity = createRefBookEntity();
        Structure structure = new Structure();

        RefBookVersionEntity entity = strategy.create(refBookEntity, structure, null);

        assertEquals(refBookEntity, entity.getRefBook());
        assertEquals(RefBookVersionStatus.PUBLISHED, entity.getStatus());
        assertEquals(firstVersion, entity.getVersion());
        assertNotNull(entity.getFromDate());

        assertNull(entity.getPassportValues());
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode("test_code");

        return refBookEntity;
    }
}