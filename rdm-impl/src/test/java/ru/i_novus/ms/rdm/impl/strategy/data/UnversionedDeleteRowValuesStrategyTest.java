package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.strategy.data.api.DeleteRowValuesStrategy;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class UnversionedDeleteRowValuesStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedDeleteRowValuesStrategy strategy;

    @Mock
    private DeleteRowValuesStrategy deleteRowValuesStrategy;

    @Test
    public void testDelete() {

        final RefBookVersionEntity entity = createUnversionedEntity();
        final List<Object> systemIds = List.of(1L, 2L);

        strategy.delete(entity, systemIds);

        verify(deleteRowValuesStrategy).delete(eq(entity), eq(systemIds));
    }
}