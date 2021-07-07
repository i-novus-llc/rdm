package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.CHANGE_ATTRIBUTE;

public class UnversionedCreateAttributeStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedCreateAttributeStrategy strategy;

    @Mock
    private CreateAttributeStrategy createAttributeStrategy;

    @Mock
    private UnversionedChangeStructureStrategy unversionedChangeStructureStrategy;

    @Test
    public void testCreate() {

        RefBookVersionEntity entity = createUnversionedEntity();
        fillOptLockValue(entity, DRAFT_OPT_LOCK_VALUE);
        when(unversionedChangeStructureStrategy.hasReferrerVersions(entity)).thenReturn(true);

        final Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        CreateAttributeRequest request = new CreateAttributeRequest(DRAFT_OPT_LOCK_VALUE, newAttribute, null);

        when(createAttributeStrategy.create(entity, request)).thenReturn(newAttribute);

        Structure.Attribute attribute = strategy.create(entity, request);
        assertEquals(newAttribute, attribute); // Добавленный атрибут

        verify(createAttributeStrategy).create(entity, request);

        verify(unversionedChangeStructureStrategy).hasReferrerVersions(entity);
        verify(unversionedChangeStructureStrategy).validatePrimariesEquality(
                eq(entity.getRefBook().getCode()), eq(entity.getStructure()), any(Structure.class)
        );
        verify(unversionedChangeStructureStrategy).processReferrers(entity);

        verifyNoMoreInteractions(createAttributeStrategy, unversionedChangeStructureStrategy);
    }
}