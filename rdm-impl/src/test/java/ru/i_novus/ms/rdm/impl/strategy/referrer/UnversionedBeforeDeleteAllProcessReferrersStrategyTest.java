package ru.i_novus.ms.rdm.impl.strategy.referrer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedBeforeDeleteAllProcessReferrersStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedBeforeDeleteAllProcessReferrersStrategy strategy;

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
    public void testDeleteAll() {

        final RefBookVersionEntity entity = createUnversionedEntity();

        // .processReferrers
        final RefBookVersionEntity referrer = createReferrerVersionEntity();
        final List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        // .processReferrer
        final List<RowValue> refRowValues = List.of(
                createReferrerRowValue(1L, 1),
                createReferrerRowValue(2L, 2)
        );
        final DataPage<RowValue> refPagedData = new DataPage<>(refRowValues.size(), refRowValues, null);
        when(searchDataService.getPagedData(any()))
                .thenReturn(refPagedData) // page with referrer data // .processReferrer
                .thenReturn(new DataPage<>(1, emptyList(), null)); // stop

        strategy.apply(entity);

        verifyFindReferrers(versionRepository);

        verify(searchDataService, times(2)).getPagedData(any());

        // .processReferrer
        final ArgumentCaptor<List<RefBookConflictEntity>> toSaveCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).saveAll(toSaveCaptor.capture());
        final List<RefBookConflictEntity> toSave = toSaveCaptor.getValue();
        assertNotNull(toSave);
        assertEquals(2, toSave.size());

        verify(conflictRepository).deleteByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdIsNotNull(
                referrer.getId(), entity.getId()
        );

        verifyNoMoreInteractions(versionRepository, conflictRepository, draftDataService);
    }
}