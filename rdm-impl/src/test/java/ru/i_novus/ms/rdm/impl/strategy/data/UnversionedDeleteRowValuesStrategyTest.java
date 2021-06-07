package ru.i_novus.ms.rdm.impl.strategy.data;

import net.n2oapp.criteria.api.CollectionPage;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.api.util.RowUtils.toLongSystemIds;

public class UnversionedDeleteRowValuesStrategyTest extends UnversionedBaseRowValuesStrategyTest {

    @InjectMocks
    private UnversionedDeleteRowValuesStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private SearchDataService searchDataService;

    @Test
    public void testDelete() {

        RefBookVersionEntity entity = createDraftEntity();

        Integer referredId = 3;
        List<Object> systemIds = List.of(1L, 2L);

        LongRowValue rowValue = createRowValue(1L, referredId);

        CollectionPage<RowValue> pagedData = new CollectionPage<>();
        pagedData.init(1, List.of(rowValue));

        // .before
        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        Long refSystemId = referredId * REFERRER_SYSTEM_ID_MULTIPLIER;
        LongRowValue refRowValue = createReferrerRowValue(refSystemId, referredId);
        CollectionPage<RowValue> refPagedData = new CollectionPage<>();
        refPagedData.init(1, List.of(refRowValue));

        when(searchDataService.getPagedData(any()))
                .thenReturn(pagedData) // page with entity data
                .thenReturn(refPagedData) // page with referrer data
                .thenReturn(new CollectionPage<>(1, emptyList(), null)); // stop

        strategy.delete(entity, systemIds);

        // .delete
        verify(draftDataService).deleteRows(eq(DRAFT_CODE), eq(systemIds));

        // .super.before
        verify(conflictRepository)
                .deleteByReferrerVersionIdAndRefRecordIdIn(eq(entity.getId()), eq(toLongSystemIds(systemIds)));

        // .before
        verifyFindReferrers(versionRepository);

        verify(conflictRepository)
                .deleteByReferrerVersionIdAndRefRecordIdIn(eq(referrer.getId()), eq(singletonList(refSystemId)));

        verify(searchDataService, times(3)).getPagedData(any());

        verify(conflictRepository).saveAll(anyList());

        verifyNoMoreInteractions(versionRepository, conflictRepository, draftDataService, searchDataService);
    }
}