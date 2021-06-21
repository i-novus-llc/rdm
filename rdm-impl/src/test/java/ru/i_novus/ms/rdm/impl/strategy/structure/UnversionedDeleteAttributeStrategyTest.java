package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.CHANGE_ATTRIBUTE;

public class UnversionedDeleteAttributeStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedDeleteAttributeStrategy strategy;

    @Mock
    private DeleteAttributeStrategy deleteAttributeStrategy;

    @Mock
    private UnversionedChangeStructureStrategy unversionedChangeStructureStrategy;

    @Test
    public void testDelete() {

        RefBookVersionEntity entity = createUnversionedEntity();
        fillOptLockValue(entity, DRAFT_OPT_LOCK_VALUE);

        final Structure.Attribute oldAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        entity.getStructure().add(oldAttribute, null);

        final String oldAttributeCode = oldAttribute.getCode();
        DeleteAttributeRequest request = new DeleteAttributeRequest(DRAFT_OPT_LOCK_VALUE, oldAttributeCode);

        when(deleteAttributeStrategy.delete(entity, request)).thenReturn(oldAttribute);

        Structure.Attribute attribute = strategy.delete(entity, request);
        assertEquals(oldAttribute, attribute); // Удалённый атрибут

        verify(deleteAttributeStrategy).delete(entity, request);
        verify(unversionedChangeStructureStrategy).processReferrers(entity);

        verifyNoMoreInteractions(deleteAttributeStrategy, unversionedChangeStructureStrategy);
    }
}