package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.strategy.data.api.DeleteAllRowValuesStrategy;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedDeleteAllRowValuesStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedDeleteAllRowValuesStrategy strategy;

    @Mock
    private DeleteAllRowValuesStrategy deleteAllRowValuesStrategy;

    @Test
    public void testDeleteAll() {

        final RefBookVersionEntity entity = createUnversionedEntity();

        strategy.deleteAll(entity);

        verify(deleteAllRowValuesStrategy).deleteAll(eq(entity));
    }
}