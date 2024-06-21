package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.DefaultBaseStrategyTest;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.math.BigInteger;
import java.util.List;

import static java.util.Collections.emptyList;

public class DefaultAfterUpdateRowValuesStrategyTest extends DefaultBaseStrategyTest {

    @InjectMocks
    private DefaultAfterUpdateRowValuesStrategy strategy;

    @Test
    public void testApply() {

        final RefBookVersionEntity entity = createDraftEntity();

        final List<RowValue> oldRowValues = List.of(
                createRowValue(1L, 1, "old"),
                createRowValue(2L, 2, "old")
        );

        final List<RowValue> newRowValues = List.of(
                createRowValue(1L, 1, "new"),
                createRowValue(2L, 2, "new")
        );

        strategy.apply(entity, emptyList(), oldRowValues, newRowValues);
    }

    private LongRowValue createRowValue(Long systemId, Integer id, String prefix) {

        return createRowValue(systemId, BigInteger.valueOf(id), prefix + "_" + id, prefix + "_text_" + id);
    }
}