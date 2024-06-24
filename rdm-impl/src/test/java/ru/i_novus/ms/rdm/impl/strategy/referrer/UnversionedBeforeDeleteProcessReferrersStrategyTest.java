package ru.i_novus.ms.rdm.impl.strategy.referrer;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UnversionedBeforeDeleteProcessReferrersStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedBeforeDeleteProcessReferrersStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private SearchDataService searchDataService;

    @Test
    @SuppressWarnings("unchecked")
    public void testDelete() {

        final RefBookVersionEntity entity = createUnversionedEntity();

        final Integer referredId = 3;
        final List<Object> systemIds = List.of(1L, 2L);

        final LongRowValue rowValue = createRowValue(1L, referredId);
        final DataPage<RowValue> pagedData = new DataPage<>(1, List.of(rowValue), null);

        // .processReferrers
        final RefBookVersionEntity referrer = createReferrerVersionEntity();
        final List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        final Long refSystemId = referredId * REFERRER_SYSTEM_ID_MULTIPLIER;
        final LongRowValue refRowValue = createReferrerRowValue(refSystemId, referredId);
        final DataPage<RowValue> refPagedData = new DataPage<>(1, List.of(refRowValue), null);
        when(searchDataService.getPagedData(any()))
                .thenReturn(pagedData) // page with entity data // .findDeletedRowValues
                .thenReturn(refPagedData) // page with referrer data // .processReferrer
                .thenReturn(new DataPage<>(1, emptyList(), null)); // stop

        // .delete
        strategy.apply(entity, systemIds);

        verifyFindReferrers(versionRepository);

        verify(conflictRepository)
                .deleteByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdIn(
                        eq(referrer.getId()), eq(entity.getId()), eq(singletonList(refSystemId))
                );

        verify(searchDataService, times(3)).getPagedData(any());

        // .processReferrer
        ArgumentCaptor<List<RefBookConflictEntity>> toSaveCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).saveAll(toSaveCaptor.capture());
        List<RefBookConflictEntity> toSave = toSaveCaptor.getValue();
        assertNotNull(toSave);
        assertEquals(1, toSave.size());

        verifyNoMoreInteractions(versionRepository, conflictRepository, draftDataService, searchDataService);
    }
}