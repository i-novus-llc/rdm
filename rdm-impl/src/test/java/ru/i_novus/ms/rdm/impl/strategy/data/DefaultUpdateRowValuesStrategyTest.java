package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.math.BigInteger;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultUpdateRowValuesStrategyTest extends DefaultBaseRowValuesStrategyTest {

    @InjectMocks
    private DefaultUpdateRowValuesStrategy strategy;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Test
    public void testUpdate() {

        RefBookVersionEntity entity = createDraftEntity();

        List<RowValue> oldRowValues = List.of(
                createRowValue(1L, 1, "old"),
                createRowValue(2L, 2, "old")
        );

        List<RowValue> newRowValues = List.of(
                createRowValue(1L, 1, "new"),
                createRowValue(2L, 2, "new")
        );

        strategy.update(entity, oldRowValues, newRowValues);

        verify(draftDataService).updateRows(eq(DRAFT_CODE), eq(newRowValues));

        List<Long> systemIds = RowUtils.toSystemIds(newRowValues);
        verify(conflictRepository)
                .deleteByReferrerVersionIdAndRefRecordIdIn(eq(entity.getId()), eq(systemIds));

        verifyNoMoreInteractions(conflictRepository, draftDataService);
    }

    private LongRowValue createRowValue(Long systemId, Integer id, String prefix) {

        return createRowValue(systemId, BigInteger.valueOf(id), prefix + "_" + id, prefix + "_text_" + id);
    }
}