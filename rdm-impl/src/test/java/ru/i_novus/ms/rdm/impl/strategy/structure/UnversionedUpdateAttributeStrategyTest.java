package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.CHANGE_ATTRIBUTE;

public class UnversionedUpdateAttributeStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedUpdateAttributeStrategy strategy;

    @Mock
    private UpdateAttributeStrategy updateAttributeStrategy;

    @Mock
    private UnversionedChangeStructureStrategy unversionedChangeStructureStrategy;

    @Test
    public void testUpdate() {

        RefBookVersionEntity entity = createUnversionedEntity();
        fillOptLockValue(entity, DRAFT_OPT_LOCK_VALUE);

        final Structure.Attribute oldAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        entity.getStructure().add(oldAttribute, null);

        final Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        newAttribute.setName(newAttribute.getName() + "_update");

        UpdateAttributeRequest request = new UpdateAttributeRequest(DRAFT_OPT_LOCK_VALUE, newAttribute, null);

        when(updateAttributeStrategy.update(entity, request)).thenReturn(newAttribute);

        Structure.Attribute attribute = strategy.update(entity, request);
        assertEquals(newAttribute, attribute); // Изменённый атрибут

        verify(updateAttributeStrategy).update(entity, request);
        verify(unversionedChangeStructureStrategy).processReferrers(entity);

        verifyNoMoreInteractions(updateAttributeStrategy, unversionedChangeStructureStrategy);
    }
}