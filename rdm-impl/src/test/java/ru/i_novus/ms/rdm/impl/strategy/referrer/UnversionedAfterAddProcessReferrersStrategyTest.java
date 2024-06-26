package ru.i_novus.ms.rdm.impl.strategy.referrer;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.ID_ATTRIBUTE_CODE;

public class UnversionedAfterAddProcessReferrersStrategyTest extends UnversionedBaseStrategyTest {

    private static final String NAME_FIELD_DELETED_VALUE_SUFFIX = "_deleted";

    @InjectMocks
    private UnversionedAfterAddProcessReferrersStrategy strategy;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private SearchDataService searchDataService;

    @Test
    @SuppressWarnings("unchecked")
    public void testAdd() {

        final RefBookVersionEntity entity = createUnversionedEntity();

        final List<RowValue> rowValues = List.of(
                // Без существующего конфликта:
                createRowValue(null, 1), // Добавление произвольной записи
                // С существующим конфликтом:
                createRowValue(null, 2), // Восстановление оригинальной записи
                createRowValue(null, 3)  // Добавление записи в изменённом виде
        );

        final List<RowValue> addedRowValues = rowValues.stream()
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return new LongRowValue(idValue.longValue(), rowValue.getFieldValues());
                })
                .collect(toList());

        final DataPage<RowValue> pagedData = new DataPage<>(addedRowValues.size(), addedRowValues, null);

        // .processReferrers
        final RefBookVersionEntity referrer = createReferrerVersionEntity();
        final List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        final List<RowValue> refRowValues = rowValues.stream()
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createReferrerRowValue(
                            idValue.longValue() * REFERRER_SYSTEM_ID_MULTIPLIER, idValue.intValue()
                    );
                })
                .collect(toList());
        refRowValues.stream()
                .filter(rowValue -> Objects.equals(rowValue.getSystemId(), 3 * REFERRER_SYSTEM_ID_MULTIPLIER))
                .forEach(rowValue -> {
                    Reference reference = (Reference) rowValue.getFieldValue(REFERRER_ATTRIBUTE_CODE).getValue();
                    reference.setDisplayValue(reference.getDisplayValue() + NAME_FIELD_DELETED_VALUE_SUFFIX);
                });

        final DataPage<RowValue> refPagedData = new DataPage<>(refRowValues.size(), refRowValues, null);
        when(searchDataService.getPagedData(any()))
                .thenReturn(pagedData) // page with entity data // .findAddedRowValues
                .thenReturn(refPagedData) // page with referrer data // .processReferrer
                .thenReturn(new DataPage<>(1, emptyList(), null)); // stop

        // .recalculateDataConflicts
        final List<RefBookConflictEntity> conflicts = List.of(
                new RefBookConflictEntity(referrer, entity,
                        2 * REFERRER_SYSTEM_ID_MULTIPLIER, REFERRER_ATTRIBUTE_CODE, ConflictType.DELETED),
                new RefBookConflictEntity(referrer, entity,
                        3 * REFERRER_SYSTEM_ID_MULTIPLIER, REFERRER_ATTRIBUTE_CODE, ConflictType.DELETED)
        );

        final List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);
        when(conflictRepository.findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIn(
                eq(REFERRER_VERSION_ID), eq(REFERRER_ATTRIBUTE_CODE), eq(ConflictType.DELETED), eq(refRecordIds)
        ))
                .thenReturn(conflicts);

        // .add
        strategy.apply(entity, rowValues);

        verifyFindReferrers(versionRepository);

        verify(searchDataService, times(3)).getPagedData(any());

        // .recalculateDataConflicts
        verify(conflictRepository).findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIn(
                eq(REFERRER_VERSION_ID), eq(REFERRER_ATTRIBUTE_CODE), eq(ConflictType.DELETED), eq(refRecordIds)
        );

        final ArgumentCaptor<List<RefBookConflictEntity>> toUpdateCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).saveAll(toUpdateCaptor.capture());
        final List<RefBookConflictEntity> toUpdate = toUpdateCaptor.getValue();
        assertNotNull(toUpdate);
        assertEquals(1, toUpdate.size());

        final ArgumentCaptor<List<RefBookConflictEntity>> toDeleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).deleteAll(toDeleteCaptor.capture());
        final List<RefBookConflictEntity> toDelete = toDeleteCaptor.getValue();
        assertNotNull(toDelete);
        assertEquals(1, toDelete.size());

        verifyNoMoreInteractions(versionRepository, conflictRepository, draftDataService, searchDataService);
    }
}