package ru.i_novus.ms.rdm.impl.strategy.data;

import net.n2oapp.criteria.api.CollectionPage;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
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
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.ID_ATTRIBUTE_CODE;

public class UnversionedUpdateRowValuesStrategyTest extends UnversionedBaseRowValuesStrategyTest {

    protected static final String NAME_FIELD_RESTORED_VALUE_SUFFIX = "_restored";
    protected static final String NAME_FIELD_CHANGED_VALUE_SUFFIX = "_changed";

    @InjectMocks
    private UnversionedUpdateRowValuesStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private SearchDataService searchDataService;

    @Mock
    private UpdateRowValuesStrategy updateRowValuesStrategy;

    @Mock
    private UnversionedAddRowValuesStrategy unversionedAddRowValuesStrategy;

    @Mock
    private UnversionedDeleteRowValuesStrategy unversionedDeleteRowValuesStrategy;

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdate() {

        RefBookVersionEntity entity = createDraftEntity();

        List<RowValue> oldRowValues = List.of(
                // Без изменения:
                createRowValue(1L, 1), // Запись без изменения
                // С изменением значений непервичных атрибутов:
                createRowValue(2L, 2), // Восстановление записи без конфликта обновления
                createRowValue(3L, 3), // Восстановление записи с конфликтом обновления
                createRowValue(4L, 4), // Изменение записи без конфликта обновления
                createRowValue(5L, 5), // Изменение записи с конфликтом обновления
                // С изменением значения первичного ключа:
                createRowValue(6L, 6)  // Изменение записи в виде удаление + добавление
        );

        List<RowValue> newRowValues = oldRowValues.stream()
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createUpdatedRowValue(idValue.intValue());
                })
                .collect(toList());

        // .processReferrers
        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        List<RowValue> refRowValues = oldRowValues.stream()
                .filter(rowValue -> !rowValue.getSystemId().equals(6L))
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createReferrerRowValue(
                            idValue.longValue() * REFERRER_SYSTEM_ID_MULTIPLIER, idValue.intValue()
                    );
                })
                .filter(Objects::nonNull)
                .collect(toList());
        refRowValues.stream()
                .filter(rowValue ->
                        List.of(2 * REFERRER_SYSTEM_ID_MULTIPLIER, 3 * REFERRER_SYSTEM_ID_MULTIPLIER)
                                .contains((Long) rowValue.getSystemId())
                )
                .forEach(rowValue -> {
                    Reference reference = (Reference) rowValue.getFieldValue(REFERRER_ATTRIBUTE_CODE).getValue();
                    reference.setDisplayValue(reference.getDisplayValue() + NAME_FIELD_RESTORED_VALUE_SUFFIX);
                });

        CollectionPage<RowValue> refPagedData = new CollectionPage<>();
        refPagedData.init(1, refRowValues);

        when(searchDataService.getPagedData(any()))
                .thenReturn(refPagedData) // page with referrer data // .processReferrer
                .thenReturn(new CollectionPage<>(1, emptyList(), null)); // stop

        // .recalculateDataConflicts
        List<RefBookConflictEntity> conflicts = List.of(
                new RefBookConflictEntity(referrer, entity,
                        3 * REFERRER_SYSTEM_ID_MULTIPLIER, REFERRER_ATTRIBUTE_CODE, ConflictType.UPDATED),
                new RefBookConflictEntity(referrer, entity,
                        5 * REFERRER_SYSTEM_ID_MULTIPLIER, REFERRER_ATTRIBUTE_CODE, ConflictType.UPDATED)
        );

        List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);
        when(conflictRepository.findByReferrerVersionIdAndRefRecordIdInAndRefFieldCodeAndConflictType(
                eq(REFERRER_VERSION_ID), eq(refRecordIds), eq(REFERRER_ATTRIBUTE_CODE), eq(ConflictType.UPDATED)
        ))
                .thenReturn(conflicts);

        // .update
        strategy.update(entity, oldRowValues, newRowValues);
        verify(updateRowValuesStrategy).update(eq(entity),
                eq(oldRowValues.subList(0, oldRowValues.size())),
                eq(newRowValues.subList(0, newRowValues.size()))
        );

        verifyFindReferrers(versionRepository);

        verify(searchDataService, times(2)).getPagedData(any());

        // .recalculateDataConflicts
        verify(conflictRepository).findByReferrerVersionIdAndRefRecordIdInAndRefFieldCodeAndConflictType(
                eq(REFERRER_VERSION_ID), eq(refRecordIds), eq(REFERRER_ATTRIBUTE_CODE), eq(ConflictType.UPDATED)
        );

        ArgumentCaptor<List<RefBookConflictEntity>> toAddCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).saveAll(toAddCaptor.capture());
        List<RefBookConflictEntity> toAdd = toAddCaptor.getValue();
        assertNotNull(toAdd);
        assertEquals(1, toAdd.size());

        ArgumentCaptor<List<RefBookConflictEntity>> toDeleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).deleteAll(toDeleteCaptor.capture());
        List<RefBookConflictEntity> toDelete = toDeleteCaptor.getValue();
        assertNotNull(toDelete);
        assertEquals(1, toDelete.size());

        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        verify(unversionedAddRowValuesStrategy)
                .processReferrers(eq(entity), eq(primaries),
                        eq(newRowValues.subList(newRowValues.size() - 1, newRowValues.size()))
                );
        verify(unversionedDeleteRowValuesStrategy)
                .processReferrers(eq(entity), eq(primaries),
                        eq(oldRowValues.subList(oldRowValues.size() - 1, oldRowValues.size()))
                );

        verifyNoMoreInteractions(versionRepository, conflictRepository, draftDataService, searchDataService,
                unversionedAddRowValuesStrategy, unversionedDeleteRowValuesStrategy);
    }

    private RowValue createUpdatedRowValue(Integer id) {

        return switch (id) {
            case 1 -> createRowValue(id.longValue(), id);
            case 2, 3 -> createRowValue(id.longValue(), BigInteger.valueOf(id),
                    NAME_FIELD_VALUE_PREFIX + id + NAME_FIELD_RESTORED_VALUE_SUFFIX, TEXT_FIELD_VALUE_PREFIX + id);
            case 4, 5 -> createRowValue(id.longValue(), BigInteger.valueOf(id),
                    NAME_FIELD_VALUE_PREFIX + id + NAME_FIELD_CHANGED_VALUE_SUFFIX, TEXT_FIELD_VALUE_PREFIX + id);
            case 6 -> createRowValue(id.longValue() + 1, BigInteger.valueOf(id),
                                NAME_FIELD_CHANGED_VALUE_SUFFIX + id, TEXT_FIELD_VALUE_PREFIX + id);
            default -> null;
        };
    }
}