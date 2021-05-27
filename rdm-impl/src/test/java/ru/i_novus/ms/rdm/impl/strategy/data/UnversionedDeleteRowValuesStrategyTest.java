package ru.i_novus.ms.rdm.impl.strategy.data;

import net.n2oapp.criteria.api.CollectionPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.version.ReferrerVersionCriteria;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.api.util.RowUtils.toLongSystemIds;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.NAME_ATTRIBUTE_CODE;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedDeleteRowValuesStrategyTest extends BaseRowValuesStrategyTest {

    private static final int REFERRER_ID = 10;
    private static final String REFERRER_CODE = "refer";

    private static final int REFERRER_VERSION_ID = 22;
    protected static final String REFERRER_VERSION_CODE = "refer_code";
    private static final String REFERRER_ATTRIBUTE_CODE = "ref";
    private static final Long REFERRER_SYSTEM_ID = 11L;

    @InjectMocks
    private UnversionedDeleteRowValuesStrategy strategy;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private SearchDataService searchDataService;

    @Test
    public void testDelete() {

        RefBookVersionEntity entity = createDraftEntity();

        List<Object> systemIds = List.of(1L, 2L);

        // .after
        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);

        final RefBookStatusType referrerStatus = RefBookStatusType.USED;
        final RefBookSourceType referrerSource = RefBookSourceType.ALL;
        ReferrerVersionCriteria referrerCriteria = new ReferrerVersionCriteria(REFBOOK_CODE, referrerStatus, referrerSource);

        when(versionRepository.findReferrerVersions(eq(REFBOOK_CODE),
                eq(referrerStatus.name()), eq(referrerSource.name()), any(Pageable.class))
        ).thenReturn(new PageImpl<>(referrers, referrerCriteria, 1)) // referrers
                .thenReturn(new PageImpl<>(emptyList(), referrerCriteria, 1)); // stop

        RefBookRowValue row = createReferrerRowValue();

        CollectionPage<RowValue> pagedData = new CollectionPage<>();
        pagedData.init(1, List.of(row));
        when(searchDataService.getPagedData(any())).thenReturn(pagedData) // data
                .thenReturn(new CollectionPage<>(1, emptyList(), null)); // stop

        strategy.delete(entity, systemIds);

        // .delete
        verify(draftDataService).deleteRows(eq(DRAFT_CODE), eq(systemIds));

        // .before
        verify(conflictRepository)
                .deleteByReferrerVersionIdAndRefRecordIdIn(eq(entity.getId()), eq(toLongSystemIds(systemIds)));

        // .after
        verify(versionRepository, times(2)).findReferrerVersions(eq(REFBOOK_CODE),
                        eq(referrerStatus.name()), eq(referrerSource.name()), any(Pageable.class));

        verify(conflictRepository)
                .deleteByReferrerVersionIdAndRefRecordIdIn(eq(referrer.getId()), eq(singletonList(REFERRER_SYSTEM_ID)));

        verify(searchDataService, times(2)).getPagedData(any());

        verify(conflictRepository).saveAll(anyList());

        verifyNoMoreInteractions(conflictRepository, draftDataService, versionRepository, searchDataService);
    }

    private RefBookVersionEntity createReferrerVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(REFERRER_VERSION_ID);
        entity.setRefBook(createReferrerEntity());
        entity.setStructure(createReferrerStructure());
        entity.setStorageCode(REFERRER_VERSION_CODE);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);

        return entity;
    }

    private RefBookEntity createReferrerEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFERRER_ID);
        entity.setCode(REFERRER_CODE);

        return entity;
    }

    private Structure createReferrerStructure() {

        Structure.Attribute refAttribute = Structure.Attribute.build(REFERRER_ATTRIBUTE_CODE, REFERRER_ATTRIBUTE_CODE, FieldType.REFERENCE, null);
        Structure.Reference refReference = new Structure.Reference(REFERRER_ATTRIBUTE_CODE, REFBOOK_CODE, DisplayExpression.toPlaceholder(NAME_ATTRIBUTE_CODE));

        Structure structure = new Structure();
        structure.add(refAttribute, refReference);

        return structure;
    }

    private RefBookRowValue createReferrerRowValue() {

        RefBookRowValue rowValue = new RefBookRowValue();
        rowValue.setSystemId(REFERRER_SYSTEM_ID);

        Reference reference = new Reference("1", "one");

        rowValue.setFieldValues(singletonList(
                new ReferenceFieldValue(REFERRER_ATTRIBUTE_CODE, reference)
        ));

        return rowValue;
    }
}