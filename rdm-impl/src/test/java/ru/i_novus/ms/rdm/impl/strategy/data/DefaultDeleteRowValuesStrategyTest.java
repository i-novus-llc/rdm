package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.i_novus.ms.rdm.api.util.RowUtils.toLongSystemIds;

public class DefaultDeleteRowValuesStrategyTest extends DefaultBaseRowValuesStrategyTest {

    @InjectMocks
    private DefaultDeleteRowValuesStrategy strategy;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Test
    public void testDelete() {

        RefBookVersionEntity entity = createDraftEntity();

        List<Object> systemIds = List.of(1L, 2L);

        strategy.delete(entity, systemIds);

        verify(draftDataService).deleteRows(eq(DRAFT_CODE), eq(systemIds));

        verify(conflictRepository)
                .deleteByReferrerVersionIdAndRefRecordIdIn(eq(entity.getId()), eq(toLongSystemIds(systemIds)));

        verifyNoMoreInteractions(conflictRepository, draftDataService);
    }
}