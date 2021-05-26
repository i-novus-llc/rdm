package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultDeleteAllRowValuesStrategyTest extends BaseRowValuesStrategyTest {

    @InjectMocks
    private DefaultDeleteAllRowValuesStrategy strategy;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Test
    public void testDeleteAll() {

        RefBookVersionEntity entity = createDraftEntity();

        strategy.deleteAll(entity);

        verify(draftDataService).deleteAllRows(eq(DRAFT_CODE));

        verify(conflictRepository).deleteByReferrerVersionIdAndRefRecordIdIsNotNull(eq(entity.getId()));

        verifyNoMoreInteractions(conflictRepository, draftDataService);
    }
}