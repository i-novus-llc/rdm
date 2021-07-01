package ru.i_novus.ms.rdm.impl.strategy.structure;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.*;

public class UnversionedChangeStructureStrategyTest extends UnversionedBaseStrategyTest {

    @InjectMocks
    private UnversionedChangeStructureStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private SearchDataService searchDataService;

    @Mock
    private VersionValidation versionValidation;

    @Test
    public void testHasReferrerVersions() {

        RefBookVersionEntity entity = createUnversionedEntity();

        when(versionValidation.hasReferrerVersions(entity.getRefBook().getCode())).thenReturn(true);

        boolean result = strategy.hasReferrerVersions(entity);
        assertTrue(result);
    }

    @Test
    public void testValidatePrimariesEquality() {

        RefBookVersionEntity entity = createUnversionedEntity();
        Structure oldStructure = entity.getStructure();
        Structure newStructure = new Structure(oldStructure);

        final Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        newStructure.add(newAttribute, null);

        when(versionValidation.equalsPrimaries(oldStructure.getPrimaries(), newStructure.getPrimaries())).thenReturn(true);
        validateSuccess(
                () -> strategy.validatePrimariesEquality(entity.getRefBook().getCode(), oldStructure, newStructure)
        );

        verify(versionValidation).equalsPrimaries(oldStructure.getPrimaries(), newStructure.getPrimaries());

        verifyNoMore();
    }

    @Test
    public void testValidatePrimariesEqualityFail() {

        RefBookVersionEntity entity = createUnversionedEntity();
        Structure oldStructure = entity.getStructure();
        Structure newStructure = new Structure(oldStructure);

        final Structure.Attribute newAttribute = new Structure.Attribute(CHANGE_ATTRIBUTE);
        newStructure.add(newAttribute, null);

        when(versionValidation.equalsPrimaries(oldStructure.getPrimaries(), newStructure.getPrimaries())).thenReturn(false);
        validateFailure(
                () -> strategy.validatePrimariesEquality(entity.getRefBook().getCode(), oldStructure, newStructure),
                UserException.class,
                "compare.structures.primaries.not.match"
        );

        newStructure.clearPrimary();
        validateFailure(
                () -> strategy.validatePrimariesEquality(entity.getRefBook().getCode(), oldStructure, newStructure),
                UserException.class,
                "compare.new.structure.primaries.not.found"
        );

        oldStructure.clearPrimary();
        validateFailure(
                () -> strategy.validatePrimariesEquality(entity.getRefBook().getCode(), oldStructure, newStructure),
                UserException.class,
                "compare.old.structure.primaries.not.found"
        );

        verify(versionValidation).equalsPrimaries(anyList(), anyList());

        verifyNoMore();
    }

    @Test
    public void testProcessSkip() {

        RefBookVersionEntity entity = createUnversionedEntity();
        entity.getStructure().clearPrimary();

        strategy.processReferrers(entity);

        verifyNoMore();
    }

    @Test
    public void testProcessDisplayDamaged() {

        RefBookVersionEntity entity = createUnversionedEntity();

        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        when(searchDataService.getPagedData(any()))
                .thenReturn(new CollectionPage<>(1, emptyList(), null)); // no referrer data

        strategy.processReferrers(entity);

        verifyFindReferrers(versionRepository);

        verify(conflictRepository)
                .findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIsNull(
                        referrer.getId(), REFERRER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED
                );

        verify(searchDataService).getPagedData(any());

        verifyNoMore();
    }

    @Test
    public void testProcessDisplayDamagedWithError() {

        RefBookVersionEntity entity = createUnversionedEntity();

        RefBookVersionEntity referrer = createReferrerVersionEntity();
        Structure.Reference reference = referrer.getStructure().getReference(REFERRER_ATTRIBUTE_CODE);
        reference.setDisplayExpression(DisplayExpression.toPlaceholder(UNKNOWN_ATTRIBUTE_CODE));

        List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        when(searchDataService.getPagedData(any()))
                .thenReturn(new CollectionPage<>(1, emptyList(), null)); // no referrer data

        strategy.processReferrers(entity);

        verifyFindReferrers(versionRepository);

        verify(conflictRepository)
                .findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIsNull(
                        referrer.getId(), REFERRER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED
                );

        verify(searchDataService).getPagedData(any());

        RefBookConflictEntity added = new RefBookConflictEntity(referrer, entity,
                null, REFERRER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED);
        verify(conflictRepository).save(added);

        verifyNoMore();
    }

    @Test
    public void testProcessDisplayDamagedWithoutError() {

        RefBookVersionEntity entity = createUnversionedEntity();

        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        RefBookConflictEntity conflict = new RefBookConflictEntity(referrer, entity,
                null, REFERRER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED);
        List<RefBookConflictEntity> conflicts = singletonList(conflict);
        when(conflictRepository.findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIsNull(
                referrer.getId(), REFERRER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED
        ))
                .thenReturn(conflicts);

        when(searchDataService.getPagedData(any()))
                .thenReturn(new CollectionPage<>(1, emptyList(), null)); // no referrer data

        strategy.processReferrers(entity);

        verifyFindReferrers(versionRepository);

        verify(conflictRepository)
                .findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIsNull(
                        referrer.getId(), REFERRER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED
                );

        verify(searchDataService).getPagedData(any());

        verify(conflictRepository).deleteAll(eq(conflicts));

        verifyNoMore();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProcessAltered() {

        RefBookVersionEntity entity = createUnversionedEntity();

        List<RowValue> rowValues = List.of(
                createRowValue(1L, 1), // Ссылка на удалённую запись без конфликта
                createRowValue(2L, 2), // Ссылка на удалённую запись с конфликтом
                createRowValue(3L, 3), // Ссылка на существующую запись без конфликта
                createRowValue(4L, 4)  // Ссылка на существующую запись с конфликтом
        );

        List<RowValue> existRowValues = rowValues.stream()
                .filter(rowValue -> List.of(3L, 4L).contains((Long) rowValue.getSystemId()))
                .collect(toList());

        CollectionPage<RowValue> pagedData = new CollectionPage<>();
        pagedData.init(existRowValues.size(), existRowValues);

        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);
        mockFindReferrers(versionRepository, referrers);

        List<RowValue> refRowValues = rowValues.stream()
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createReferrerRowValue(
                            idValue.longValue() * REFERRER_SYSTEM_ID_MULTIPLIER, idValue.intValue()
                    );
                })
                .collect(toList());
        List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);

        CollectionPage<RowValue> refPagedData = new CollectionPage<>();
        refPagedData.init(refRowValues.size(), refRowValues);

        List<RefBookConflictEntity> conflicts = Stream.of(2L, 4L)
                .map(id -> id * REFERRER_SYSTEM_ID_MULTIPLIER)
                .map(refRecordId ->
                        new RefBookConflictEntity(referrer, entity,
                                refRecordId, REFERRER_ATTRIBUTE_CODE, ConflictType.ALTERED)
                )
                .collect(toList());
        when(conflictRepository.findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIn(
                referrer.getId(), REFERRER_ATTRIBUTE_CODE, ConflictType.ALTERED, refRecordIds
        ))
                .thenReturn(new ArrayList<>(conflicts));

        when(searchDataService.getPagedData(any()))
                .thenReturn(refPagedData) // page with referrer data // .processReferrer
                .thenReturn(pagedData) // page with entity data // .findReferredRowValues
                .thenReturn(new CollectionPage<>(1, emptyList(), null)); // stop

        strategy.processReferrers(entity);

        verifyFindReferrers(versionRepository);

        verify(conflictRepository)
                .findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIsNull(
                        referrer.getId(), REFERRER_ATTRIBUTE_CODE, ConflictType.DISPLAY_DAMAGED
                );

        verify(conflictRepository)
                .findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIn(
                        referrer.getId(), REFERRER_ATTRIBUTE_CODE, ConflictType.ALTERED, refRecordIds
                );

        verify(searchDataService, times(3)).getPagedData(any());

        ArgumentCaptor<List<RefBookConflictEntity>> toAddCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).saveAll(toAddCaptor.capture());
        List<RefBookConflictEntity> toUpdate = toAddCaptor.getValue();
        assertNotNull(toUpdate);
        assertEquals(1, toUpdate.size());

        ArgumentCaptor<List<RefBookConflictEntity>> toDeleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(conflictRepository).deleteAll(toDeleteCaptor.capture());
        List<RefBookConflictEntity> toDelete = toDeleteCaptor.getValue();
        assertNotNull(toDelete);
        assertEquals(1, toDelete.size());

        verifyNoMore();
    }

    private void verifyNoMore() {

        verifyNoMoreInteractions(versionRepository, conflictRepository, searchDataService, versionValidation);
    }
}