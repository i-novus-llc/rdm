package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.strategy.publish.EditPublishStrategy;
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

public class UnversionedDeleteRowValuesStrategyTest extends UnversionedBaseStrategyTest {

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

    @Mock
    private DeleteRowValuesStrategy deleteRowValuesStrategy;

    @Mock
    private EditPublishStrategy editPublishStrategy;

    @Test
    @SuppressWarnings("unchecked")
    public void testDelete() {

        RefBookVersionEntity entity = createUnversionedEntity();

        Integer referredId = 3;
        List<Object> systemIds = List.of(1L, 2L);

        LongRowValue rowValue = createRowValue(1L, referredId);

        DataPage<RowValue> pagedData = new DataPage<>(1, List.of(rowValue), null);

        // .processReferrers
        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        Long refSystemId = referredId * REFERRER_SYSTEM_ID_MULTIPLIER;
        LongRowValue refRowValue = createReferrerRowValue(refSystemId, referredId);
        DataPage<RowValue> refPagedData = new DataPage<>(1, List.of(refRowValue), null);

        when(searchDataService.getPagedData(any()))
                .thenReturn(pagedData) // page with entity data // .findDeletedRowValues
                .thenReturn(refPagedData) // page with referrer data // .processReferrer
                .thenReturn(new DataPage<>(1, emptyList(), null)); // stop

        // .delete
        strategy.delete(entity, systemIds);

        verify(deleteRowValuesStrategy).delete(eq(entity), eq(systemIds));
        verify(editPublishStrategy).publish(entity);

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

        verifyNoMoreInteractions(versionRepository, conflictRepository,
                draftDataService, searchDataService, editPublishStrategy);
    }
}