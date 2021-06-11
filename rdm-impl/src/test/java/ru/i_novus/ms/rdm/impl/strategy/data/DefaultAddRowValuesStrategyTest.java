package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultAddRowValuesStrategyTest extends DefaultBaseRowValuesStrategyTest {

    @InjectMocks
    private DefaultAddRowValuesStrategy strategy;

    @Mock
    private DraftDataService draftDataService;

    @Test
    public void testAdd() {

        RefBookVersionEntity entity = createDraftEntity();

        List<RowValue> rowValues = List.of(
                createRowValue(null, 1),
                createRowValue(null, 2)
        );

        strategy.add(entity, rowValues);

        verify(draftDataService).addRows(eq(DRAFT_CODE), eq(rowValues));

        verifyNoMoreInteractions(draftDataService);
    }
}