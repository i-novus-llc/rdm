package ru.inovus.ms.rdm.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.model.diff.RefBookDataDiff;
import ru.inovus.ms.rdm.model.field.ReferenceFilterValue;
import ru.inovus.ms.rdm.model.version.AttributeFilter;
import ru.inovus.ms.rdm.model.conflict.Conflict;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.repositiory.RefBookConflictRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.TimeUtils;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConflictServiceTest {

    private static final Integer REFERRER_REF_BOOK_ID = -1;
    private static final String REFERRER_REF_BOOK_CODE = "TEST_REFERRER_BOOK";
    private static final Integer REFERRER_VERSION_ID = -3;
    private static final String REFERRER_REFERENCE_ATTRIBUTE_CODE = "ref";
    private static final String REFERRER_REFERENCE_DISPLAY_EXPRESSION = "${name}: ${amount}";

    private static final Integer PUBLISHED_REF_BOOK_ID = -2;
    private static final String PUBLISHED_REF_BOOK_CODE = "TEST_PUBLISHED_BOOK";
    private static final Integer PUBLISHED_VERSION_ID = -4;

    private static final Integer REFERRER_DRAFT_ID = -5;
    private static final String REFERRER_DRAFT_STORAGE_CODE = "TEST_REFERRER_STORAGE";
    private static final String REFERRER_PRIMARY_CODE = "str";

    private static final Integer PUBLISHING_DRAFT_ID = -6;
    private static final String PUBLISHING_DRAFT_STORAGE_CODE = "TEST_PUBLISHING_STORAGE";
    private static final String PUBLISHING_PRIMARY_CODE = "code";

    // for `testRecalculateConflicts`:
    final Long REFERRER_PRIMARY_CODE_MULTIPLIER = 10L;

    private static final Long PUBLISHED_ROW_SYS_ID_UNCHANGED_UNCHANGING = 1L;
    private static final Long PUBLISHED_ROW_SYS_ID_UNCHANGED_UPDATING = 2L;
    private static final Long PUBLISHED_ROW_SYS_ID_UNCHANGED_DELETING = 3L;
    private static final Long PUBLISHED_ROW_SYS_ID_UPDATED_UNCHANGING = 4L; // reuse conflict
    private static final Long PUBLISHED_ROW_SYS_ID_UPDATED_UPDATING = 5L;
    private static final Long PUBLISHED_ROW_SYS_ID_UPDATED_DELETING = 6L;
    private static final Long PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING = 7L; // reuse conflict
    private static final Long PUBLISHED_ROW_SYS_ID_DELETED_RESTORING = 8L;
    private static final Long PUBLISHED_ROW_SYS_ID_DELETED_REMOLDING = 9L;  // emend conflict
    private static final Long PUBLISHED_ROW_SYS_ID_ABSENT_INSERTING = 10L;

    public static final List<Long> CONFLICTED_PUBLISHED_ROW_SYS_IDS_UPDATED = asList(
            PUBLISHED_ROW_SYS_ID_UPDATED_UNCHANGING,
            PUBLISHED_ROW_SYS_ID_UPDATED_UPDATING,
            PUBLISHED_ROW_SYS_ID_UPDATED_DELETING
    );

    public static final List<Long> CONFLICTED_PUBLISHED_ROW_SYS_IDS_DELETED = asList(
            PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING,
            PUBLISHED_ROW_SYS_ID_DELETED_RESTORING,
            PUBLISHED_ROW_SYS_ID_DELETED_REMOLDING
    );

    public static final List<Long> CONFLICTED_PUBLISHED_ROW_SYS_IDS =
            Stream.of(
                    CONFLICTED_PUBLISHED_ROW_SYS_IDS_UPDATED,
                    CONFLICTED_PUBLISHED_ROW_SYS_IDS_DELETED)
            .flatMap(List::stream)
            .collect(Collectors.toList());

    // for `testRefreshReferencesByPrimary`:
    private static final String REFERRER_PRIMARY_UNCHANGED_VALUE = "r3";
    private static final String REFERRER_PRIMARY_UPDATED_VALUE = "r2";
    private static final String REFERRER_PRIMARY_DELETED_VALUE = "r202";

    private static final String PUBLISHING_PRIMARY_UNCHANGED_VALUE = "3";
    private static final String PUBLISHING_PRIMARY_UPDATED_VALUE = "2";
    private static final String PUBLISHING_PRIMARY_DELETED_VALUE = "202";
    private static final String PUBLISHING_PRIMARY_UPDATED_DISPLAY = "Doubled_Two: 2222";

    @InjectMocks
    private ConflictServiceImpl conflictService;

    @Mock
    private VersionService versionService;
    @Mock
    private DraftService draftService;
    @Mock
    private CompareService compareService;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookConflictRepository conflictRepository;

    private RefBookVersionEntity referrerEntity;
    private RefBookVersionEntity publishedEntity;

    @Before
    public void setUp() {
        referrerEntity = createReferrerEntity(REFERRER_VERSION_ID);
        publishedEntity = createPublishingEntity(PUBLISHED_VERSION_ID);

        doNothing().when(versionValidation).validateVersionExists(anyInt());

        when(versionRepository.getOne(eq(REFERRER_VERSION_ID))).thenReturn(referrerEntity);
        when(versionRepository.getOne(eq(PUBLISHED_VERSION_ID))).thenReturn(publishedEntity);
    }

    private RefBookVersionEntity createReferrerEntity(Integer versionId) {
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(REFERRER_REF_BOOK_ID);
        refBookEntity.setCode(REFERRER_REF_BOOK_CODE);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(versionId);
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary(REFERRER_PRIMARY_CODE, "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_REFERENCE_ATTRIBUTE_CODE, "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference(REFERRER_REFERENCE_ATTRIBUTE_CODE, PUBLISHED_REF_BOOK_CODE, REFERRER_REFERENCE_DISPLAY_EXPRESSION)
                )
            );
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    private RefBookVersionEntity createPublishingEntity(Integer versionId) {
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(PUBLISHED_REF_BOOK_ID);
        refBookEntity.setCode(PUBLISHED_REF_BOOK_CODE);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(versionId);
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary(PUBLISHING_PRIMARY_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build("name", "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build("amount", "Количество", FieldType.INTEGER, "количество единиц")
                ),
                emptyList()
        );
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    @Test
    public void testCalculateConflicts() {
    }

    //@Test
    public void testRecalculateConflicts() {

        //RefBookVersionEntity referrerDraftEntity = createReferrerEntity(REFERRER_DRAFT_ID);
        RefBookVersionEntity publishingEntity = createPublishingEntity(PUBLISHING_DRAFT_ID);

        Page<RefBookConflict> conflicts = createRecalculateConflictsPage();

        when(versionRepository.getOne(eq(referrerEntity.getId()))).thenReturn(referrerEntity);
        when(versionRepository.getOne(eq(publishedEntity.getId()))).thenReturn(publishedEntity);

        Page<RefBookRowValue> refFromRowValues = createRecalculateConflictsRowValues();
        when(versionService.search(eq(referrerEntity.getId()), any())).thenReturn(refFromRowValues);

        RefBookDataDiff refBookDataDiff = null;
        when(compareService.compareData(any())).thenReturn(refBookDataDiff);

        List<Conflict> list = conflictService.recalculateConflicts(referrerEntity.getId(),
                publishedEntity.getId(), publishingEntity.getId(), conflicts);
    }
    
    private Page<RefBookConflict> createRecalculateConflictsPage() {

        List<RefBookConflict> conflicts = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());

        CONFLICTED_PUBLISHED_ROW_SYS_IDS_UPDATED.forEach(systemId -> {
            conflicts.add(new RefBookConflict(referrerEntity.getId(),
                    publishedEntity.getId(),
                    systemId,
                    REFERRER_REFERENCE_ATTRIBUTE_CODE,
                    ConflictType.UPDATED,
                    TimeUtils.now())
            );
        });

        CONFLICTED_PUBLISHED_ROW_SYS_IDS_DELETED.forEach(systemId -> {
            conflicts.add(new RefBookConflict(referrerEntity.getId(),
                    publishedEntity.getId(),
                    systemId,
                    REFERRER_REFERENCE_ATTRIBUTE_CODE,
                    ConflictType.DELETED,
                    TimeUtils.now())
            );
        });

        return new PageImpl<>(conflicts, Pageable.unpaged(), conflicts.size());
    }

    private Page<RefBookRowValue> createRecalculateConflictsRowValues () {

        List<RefBookRowValue> rowValues = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());
        CONFLICTED_PUBLISHED_ROW_SYS_IDS.forEach(systemId -> {
            LongRowValue longRowValue = new LongRowValue(systemId,
                    asList(new StringFieldValue(REFERRER_PRIMARY_CODE, getRecalculateConflictsReferrerPrimaryValue(systemId)),
                            new ReferenceFieldValue(REFERRER_REFERENCE_ATTRIBUTE_CODE, createRecalculateConflictsReferrerReference(systemId))
                    )
            );
            rowValues.add(new RefBookRowValue(longRowValue, referrerEntity.getId()));
        });

        return new PageImpl<>(rowValues, Pageable.unpaged(), rowValues.size());
    }

    private String getRecalculateConflictsReferrerPrimaryValue(Long publishedRowSystemId) {
        return String.valueOf(PUBLISHED_ROW_SYS_ID_UPDATED_UNCHANGING * REFERRER_PRIMARY_CODE_MULTIPLIER);
    }

    private String getRecalculateConflictsReferrerDisplayValue(Long publishedRowSystemId) {
        String publishedRowCode = String.valueOf(publishedRowSystemId);
        return publishedRowCode + ": " + publishedRowCode + publishedRowCode;
    }

    private Reference createRecalculateConflictsReferrerReference(Long publishedRowSystemId) {

        String publishedRowCode = String.valueOf(publishedRowSystemId);

        return new Reference(REFERRER_DRAFT_STORAGE_CODE,
                null,
                PUBLISHING_PRIMARY_CODE,
                new DisplayExpression(REFERRER_REFERENCE_DISPLAY_EXPRESSION),
                publishedRowCode,
                getRecalculateConflictsReferrerDisplayValue(publishedRowSystemId)
        );
    }

    @Test
    public void testRefreshReferencesByPrimary() {

        referrerEntity.setStorageCode(REFERRER_DRAFT_STORAGE_CODE);
        publishedEntity.setStorageCode(PUBLISHING_DRAFT_STORAGE_CODE);

        List<Conflict> conflicts = createRefreshReferencesConflicts();

        SearchDataCriteria referrerUnchangedCriteria = createRefreshReferencesReferrerCriteria(REFERRER_PRIMARY_UNCHANGED_VALUE);
        PageImpl<RefBookRowValue> referrerUnchangedRows = createRefreshReferencesReferrerRows(PUBLISHING_PRIMARY_UNCHANGED_VALUE, "Three: 33");
        when(versionService.search(eq(REFERRER_VERSION_ID), eq(referrerUnchangedCriteria))).thenReturn(referrerUnchangedRows);

        SearchDataCriteria referrerUpdatedCriteria = createRefreshReferencesReferrerCriteria(REFERRER_PRIMARY_UPDATED_VALUE);
        PageImpl<RefBookRowValue> referrerUpdatedRows = createRefreshReferencesReferrerRows(PUBLISHING_PRIMARY_UPDATED_VALUE, "Two: 22");
        when(versionService.search(eq(REFERRER_VERSION_ID), eq(referrerUpdatedCriteria))).thenReturn(referrerUpdatedRows);

        SearchDataCriteria publishingUnchangedCriteria = createRefreshReferencesPublishingCriteria(PUBLISHING_PRIMARY_UNCHANGED_VALUE);
        PageImpl<RefBookRowValue> publishingUnchangedRows = createRefreshReferencesPublishingRows("Three", 33);
        when(versionService.search(eq(PUBLISHED_VERSION_ID), eq(publishingUnchangedCriteria))).thenReturn(publishingUnchangedRows);

        SearchDataCriteria publishingUpdatedCriteria = createRefreshReferencesPublishingCriteria(PUBLISHING_PRIMARY_UPDATED_VALUE);
        PageImpl<RefBookRowValue> publishingUpdatedRows = createRefreshReferencesPublishingRows("Doubled_Two", 2222);
        when(versionService.search(eq(PUBLISHED_VERSION_ID), eq(publishingUpdatedCriteria))).thenReturn(publishingUpdatedRows);

        ArgumentCaptor<RefBookRowValue> rowValueCaptor = ArgumentCaptor.forClass(RefBookRowValue.class);

        conflictService.refreshReferencesByPrimary(REFERRER_VERSION_ID, PUBLISHED_VERSION_ID, conflicts);

        verify(draftDataService, times(1)).updateRow(eq(REFERRER_DRAFT_STORAGE_CODE), rowValueCaptor.capture());

        PageImpl<RefBookRowValue> updatedRows = createRefreshReferencesReferrerRows(PUBLISHING_PRIMARY_UPDATED_VALUE, PUBLISHING_PRIMARY_UPDATED_DISPLAY);
        RefBookRowValue updatedRow = updatedRows.get().findFirst().orElse(null);
        Assert.assertNotNull(updatedRow);

        Reference expectedReference = new Reference(
                PUBLISHING_DRAFT_STORAGE_CODE,
                null,
                PUBLISHING_PRIMARY_CODE, // referenceAttribute
                new DisplayExpression(REFERRER_REFERENCE_DISPLAY_EXPRESSION),
                PUBLISHING_PRIMARY_UPDATED_VALUE,
                PUBLISHING_PRIMARY_UPDATED_DISPLAY);
        ReferenceFieldValue expectedFieldValue = new ReferenceFieldValue(REFERRER_REFERENCE_ATTRIBUTE_CODE, expectedReference);
        LongRowValue expectedRowValue = new LongRowValue(updatedRow.getSystemId(), singletonList(expectedFieldValue));
        Assert.assertEquals(new RefBookRowValue(expectedRowValue, REFERRER_VERSION_ID), rowValueCaptor.getValue());
    }

    private List<Conflict> createRefreshReferencesConflicts() {
        // NB: Multiple primary keys are not supported yet.
        List<Conflict> conflicts = new ArrayList<>();

        List<FieldValue> unchangedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(REFERRER_PRIMARY_CODE, REFERRER_PRIMARY_UNCHANGED_VALUE)
                )
        );
        conflicts.add(new Conflict(REFERRER_REFERENCE_ATTRIBUTE_CODE, ConflictType.UPDATED, unchangedValues));

        List<FieldValue> updatedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(REFERRER_PRIMARY_CODE, REFERRER_PRIMARY_UPDATED_VALUE)
                )
        );
        conflicts.add(new Conflict(REFERRER_REFERENCE_ATTRIBUTE_CODE, ConflictType.UPDATED, updatedValues));

        List<FieldValue> deletedValues = new ArrayList<>(
                singletonList(
                        new StringFieldValue(REFERRER_PRIMARY_CODE, REFERRER_PRIMARY_DELETED_VALUE)
                )
        );
        conflicts.add(new Conflict(REFERRER_REFERENCE_ATTRIBUTE_CODE, ConflictType.DELETED, deletedValues));

        return conflicts;
    }

    private static SearchDataCriteria createRefreshReferencesReferrerCriteria(String referenceValue) {

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        AttributeFilter filter = new AttributeFilter(REFERRER_PRIMARY_CODE, referenceValue, FieldType.STRING, SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        return criteria;
    }

    private static PageImpl<RefBookRowValue> createRefreshReferencesReferrerRows(String referenceValue, String displayValue) {
        DisplayExpression displayExpression = new DisplayExpression(REFERRER_REFERENCE_DISPLAY_EXPRESSION);

        return new PageImpl<>(singletonList(
                new RefBookRowValue(new LongRowValue(
                        new StringFieldValue("str", "str-referrer"),
                        new ReferenceFieldValue(REFERRER_REFERENCE_ATTRIBUTE_CODE,
                                new Reference(PUBLISHING_DRAFT_STORAGE_CODE,
                                        null,
                                        PUBLISHING_PRIMARY_CODE,
                                        displayExpression,
                                        referenceValue,
                                        displayValue))
                ), REFERRER_VERSION_ID)
        ), PageRequest.of(0, 10), 1);
    }

    private static SearchDataCriteria createRefreshReferencesPublishingCriteria(String codeValue) {

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        AttributeFilter filter = new AttributeFilter(PUBLISHING_PRIMARY_CODE, codeValue, FieldType.STRING, SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        return criteria;
    }

    private static PageImpl<RefBookRowValue> createRefreshReferencesPublishingRows(String nameValue, Integer amountValue) {
        return new PageImpl<>(singletonList(
                new RefBookRowValue(new LongRowValue(
                        new StringFieldValue(PUBLISHING_PRIMARY_CODE, PUBLISHING_PRIMARY_UPDATED_VALUE),
                        new StringFieldValue("name", nameValue),
                        new IntegerFieldValue("amount", BigInteger.valueOf(amountValue))
                ), PUBLISHED_VERSION_ID)
        ), PageRequest.of(0, 10), 1);
    }

}