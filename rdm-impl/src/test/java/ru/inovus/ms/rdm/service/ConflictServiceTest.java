package ru.inovus.ms.rdm.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.value.*;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.diff.RefBookDataDiff;
import ru.inovus.ms.rdm.model.conflict.Conflict;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.repositiory.RefBookConflictRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConflictServiceTest {

    private static final Integer REFERRER_REF_BOOK_ID = -1;
    private static final String REFERRER_REF_BOOK_CODE = "TEST_REFERRER_BOOK";
    private static final Integer REFERRER_VERSION_ID = -3;

    private static final String REFERRER_ATTRIBUTE_CODE = "str";
    private static final String REFERRER_ATTRIBUTE_REFERENCE = "ref";
    private static final String REFERRER_REFERENCE_DISPLAY_EXPRESSION = "${name}: ${amount}";

    private static final Integer PUBLISHED_REF_BOOK_ID = -2;
    private static final String PUBLISHED_REF_BOOK_CODE = "TEST_PUBLISHED_BOOK";
    private static final Integer PUBLISHED_VERSION_ID = -4;

    private static final String PUBLISHED_ATTRIBUTE_CODE = "code";
    private static final String PUBLISHED_ATTRIBUTE_NAME = "name";
    private static final String PUBLISHED_ATTRIBUTE_AMOUNT = "amount";
    private static final String PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX = "_name";
    private static final String PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX = "01";

    private static final Integer REFERRER_DRAFT_ID = -5;
    private static final String REFERRER_DRAFT_STORAGE_CODE = "TEST_REFERRER_STORAGE";

    private static final Integer PUBLISHING_DRAFT_ID = -6;
    private static final String PUBLISHING_DRAFT_STORAGE_CODE = "TEST_PUBLISHING_STORAGE";

    // for `testRecalculateConflicts`:
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

    private static final List<Long> CONFLICTED_PUBLISHED_ROW_SYS_IDS_UPDATED = asList(
            PUBLISHED_ROW_SYS_ID_UPDATED_UNCHANGING,
            PUBLISHED_ROW_SYS_ID_UPDATED_UPDATING,
            PUBLISHED_ROW_SYS_ID_UPDATED_DELETING
    );

    private static final List<Long> CONFLICTED_PUBLISHED_ROW_SYS_IDS_DELETED = asList(
            PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING,
            PUBLISHED_ROW_SYS_ID_DELETED_RESTORING,
            PUBLISHED_ROW_SYS_ID_DELETED_REMOLDING
    );

    private static final List<Long> CONFLICTED_PUBLISHED_ROW_SYS_IDS =
            Stream.of(
                    CONFLICTED_PUBLISHED_ROW_SYS_IDS_UPDATED,
                    CONFLICTED_PUBLISHED_ROW_SYS_IDS_DELETED)
            .flatMap(List::stream)
            .collect(Collectors.toList());

    private static final List<Long> CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED = asList(
            PUBLISHED_ROW_SYS_ID_UPDATED_UNCHANGING,
            PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING,
            PUBLISHED_ROW_SYS_ID_DELETED_REMOLDING
    );

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
                        Structure.Attribute.buildPrimary(REFERRER_ATTRIBUTE_CODE, "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_ATTRIBUTE_REFERENCE, "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference(REFERRER_ATTRIBUTE_REFERENCE, PUBLISHED_REF_BOOK_CODE, REFERRER_REFERENCE_DISPLAY_EXPRESSION)
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
                        Structure.Attribute.buildPrimary(PUBLISHED_ATTRIBUTE_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build(PUBLISHED_ATTRIBUTE_NAME, "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build(PUBLISHED_ATTRIBUTE_AMOUNT, "Количество", FieldType.INTEGER, "количество единиц")
                ),
                emptyList()
        );
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    @Test
    public void testCalculateConflicts() {
    }

    /**
     * Тест перевычисления существующих конфликтов справочников.
     *
     * <p><br/>Таблица:<br/>
     * Записи в справочнике, на который ссылаются, и конфликты по этим записям.</p>
     *
     * <pre>
     * После 1-й публикации                    После 2-й публикации
     *
     *  Записи  Конфликты       Изменение       Записи  Конфликты
     *          в таблице       в записях               в таблице
     * primary                 diffConflict    primary
     *    1         -               -             1         -
     *    2         -               u             2         u
     *    3         -               d             -         d
     *    4         u               -             4        *u*
     *    5         u               u             5         u
     *    6         u               d             -         d
     *    7         d               -             -        *d*
     *    8         d               i             8         -
     *    9         d              ~i~            9        *u*
     * </pre>
     *
     * <p>Изменение в записях:<br/>
     * <strong>i</strong> - восстановление ранее удалённой строки,<br/>
     * <strong>~i~</strong> - вставка ранее удалённой строки с изменениями.</p>
     *
     * <p>Изменение в конфликтах:<br/>
     * для записей 4 и 7 сохраняется старый конфликт,<br/>
     * для записи 9 меняется тип старого конфликта с удаления на обновление.</p>
     */
    @Test
    public void testRecalculateConflicts() {

        RefBookVersionEntity publishingEntity = createPublishingEntity(PUBLISHING_DRAFT_ID);

        Page<RefBookConflictEntity> conflicts = createRecalculateConflictsPage();

        Page<RefBookRowValue> refFromRowValues = createRecalculateConflictsRowValues();
        when(versionService.search(eq(referrerEntity.getId()), any())).thenReturn(refFromRowValues);

        RefBookDataDiff refBookDataDiff = createRecalculateConflictsDataDiff();
        when(compareService.compareData(any())).thenReturn(refBookDataDiff);

        // Проверка в случае изменения только данных.
        List<RefBookConflictEntity> expectedList = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.size());
        CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.forEach(systemId -> {
            ConflictType conflictType =
                    PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING.equals(systemId) ? ConflictType.DELETED : ConflictType.UPDATED;
            expectedList.add(
                    new RefBookConflictEntity(referrerEntity, publishingEntity, null, REFERRER_ATTRIBUTE_REFERENCE, conflictType)
            );
        });

        List<RefBookConflictEntity> actualList = conflictService.recalculateConflicts(referrerEntity,
                publishedEntity, publishingEntity, conflicts.getContent(), false);
        assertConflictEntities(expectedList, actualList);

        // Проверка в случае изменения данных и структуры.
        List<RefBookConflictEntity> expectedAlteredList = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.size());
        CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.forEach(systemId -> {
            ConflictType conflictType =
                    PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING.equals(systemId) ? ConflictType.DELETED : ConflictType.UPDATED;

            if (!ConflictType.UPDATED.equals(conflictType)) {
                expectedAlteredList.add(
                        new RefBookConflictEntity(referrerEntity, publishingEntity, null, REFERRER_ATTRIBUTE_REFERENCE, conflictType)
                );
            }
        });

        List<RefBookConflictEntity> actualAlteredList = conflictService.recalculateConflicts(referrerEntity,
                publishedEntity, publishingEntity, conflicts.getContent(), true);
        assertConflictEntities(expectedAlteredList, actualAlteredList);
    }
    
    private Page<RefBookConflictEntity> createRecalculateConflictsPage() {

        List<RefBookConflictEntity> conflicts = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());

        CONFLICTED_PUBLISHED_ROW_SYS_IDS_UPDATED.forEach(systemId -> {
            conflicts.add(new RefBookConflictEntity(referrerEntity,
                    publishedEntity,
                    systemId,
                    REFERRER_ATTRIBUTE_REFERENCE,
                    ConflictType.UPDATED)
            );
        });

        CONFLICTED_PUBLISHED_ROW_SYS_IDS_DELETED.forEach(systemId -> {
            conflicts.add(new RefBookConflictEntity(referrerEntity,
                    publishedEntity,
                    systemId,
                    REFERRER_ATTRIBUTE_REFERENCE,
                    ConflictType.DELETED)
            );
        });

        return new PageImpl<>(conflicts, Pageable.unpaged(), conflicts.size());
    }

    private Page<RefBookRowValue> createRecalculateConflictsRowValues () {

        List<RefBookRowValue> rowValues = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());
        CONFLICTED_PUBLISHED_ROW_SYS_IDS.forEach(systemId -> {
            LongRowValue longRowValue = new LongRowValue(systemId,
                    asList(new StringFieldValue(REFERRER_ATTRIBUTE_CODE, getRecalculateConflictsReferrerPrimaryValue(systemId)),
                            new ReferenceFieldValue(REFERRER_ATTRIBUTE_REFERENCE, createRecalculateConflictsReferrerReference(systemId))
                    )
            );
            rowValues.add(new RefBookRowValue(longRowValue, referrerEntity.getId()));
        });

        return new PageImpl<>(rowValues, Pageable.unpaged(), rowValues.size());
    }

    private RefBookDataDiff createRecalculateConflictsDataDiff() {

        final StringField PUBLISHED_FIELD_CODE = new StringField(PUBLISHED_ATTRIBUTE_CODE);
        final StringField PUBLISHED_FIELD_NAME = new StringField(PUBLISHED_ATTRIBUTE_NAME);
        final IntegerField PUBLISHED_FIELD_AMOUNT = new IntegerField(PUBLISHED_ATTRIBUTE_AMOUNT);

        List<DiffRowValue> diffRowValues = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());
        CONFLICTED_PUBLISHED_ROW_SYS_IDS.forEach(systemId -> {
            if (systemId.equals(PUBLISHED_ROW_SYS_ID_UPDATED_UNCHANGING)
                   || systemId.equals(PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING)) {
                return;
            }

            if (systemId.equals(PUBLISHED_ROW_SYS_ID_UPDATED_UPDATING)) {
                String oldValue = getRecalculateConflictsPublishedPrimaryValue(systemId);
                String newValue = getRecalculateConflictsPublishingPrimaryValue(systemId);

                diffRowValues.add(new DiffRowValue(asList(
                        new DiffFieldValue<>(PUBLISHED_FIELD_CODE, oldValue, oldValue, null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_NAME,
                                oldValue + PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX,
                                newValue + PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX,
                                null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_AMOUNT,
                                new BigInteger(oldValue + PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX),
                                new BigInteger(newValue + PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX),
                                null)
                ), DiffStatusEnum.UPDATED));
            }

            if (systemId.equals(PUBLISHED_ROW_SYS_ID_UPDATED_DELETING)) {
                String oldValue = getRecalculateConflictsPublishedPrimaryValue(systemId);

                diffRowValues.add(new DiffRowValue(asList(
                        new DiffFieldValue<>(PUBLISHED_FIELD_CODE, oldValue, null, null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_NAME,
                                oldValue + PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX,
                                null,
                                null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_AMOUNT,
                                new BigInteger(oldValue + PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX),
                                null,
                                null)
                ), DiffStatusEnum.DELETED));
            }

            if (systemId.equals(PUBLISHED_ROW_SYS_ID_DELETED_RESTORING)) {
                String newValue = getRecalculateConflictsPublishedPrimaryValue(systemId);

                diffRowValues.add(new DiffRowValue(asList(
                        new DiffFieldValue<>(PUBLISHED_FIELD_CODE, null, newValue, null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_NAME,
                                null,
                                newValue + PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX,
                                null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_AMOUNT,
                                null,
                                new BigInteger(newValue + PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX),
                                null)
                ), DiffStatusEnum.INSERTED));
            }

            if (systemId.equals(PUBLISHED_ROW_SYS_ID_DELETED_REMOLDING)) {
                String newValue = getRecalculateConflictsPublishedPrimaryValue(systemId);
                String altValue = getRecalculateConflictsPublishingPrimaryValue(systemId);

                diffRowValues.add(new DiffRowValue(asList(
                        new DiffFieldValue<>(PUBLISHED_FIELD_CODE, null, newValue, null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_NAME,
                                null,
                                altValue + PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX,
                                null),
                        new DiffFieldValue<>(PUBLISHED_FIELD_AMOUNT,
                                null,
                                new BigInteger(altValue + PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX),
                                null)
                ), DiffStatusEnum.INSERTED));
            }
        });

        return new RefBookDataDiff(new PageImpl<>(diffRowValues, Pageable.unpaged(), diffRowValues.size()),
                null, null, null);
    }

    private String getRecalculateConflictsReferrerPrimaryValue(Long publishedRowSystemId) {
        final Long REFERRER_PRIMARY_CODE_MULTIPLIER = 10L;
        return String.valueOf(publishedRowSystemId * REFERRER_PRIMARY_CODE_MULTIPLIER);
    }

    private String getRecalculateConflictsReferrerDisplayValue(Long publishedRowSystemId) {
        String publishedRowCode = String.valueOf(publishedRowSystemId);
        return publishedRowCode + PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX + ": "
                + publishedRowCode + PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX;
    }

    private Reference createRecalculateConflictsReferrerReference(Long publishedRowSystemId) {

        String publishedRowCode = String.valueOf(publishedRowSystemId);

        return new Reference(REFERRER_DRAFT_STORAGE_CODE,
                null,
                PUBLISHED_ATTRIBUTE_CODE,
                new DisplayExpression(REFERRER_REFERENCE_DISPLAY_EXPRESSION),
                publishedRowCode,
                getRecalculateConflictsReferrerDisplayValue(publishedRowSystemId)
        );
    }

    private String getRecalculateConflictsPublishedPrimaryValue(Long publishedRowSystemId) {
        return String.valueOf(publishedRowSystemId);
    }

    private String getRecalculateConflictsPublishingPrimaryValue(Long publishedRowSystemId) {
        final Long PUBLISHING_PRIMARY_CODE_MULTIPLIER = 1001L;
        return String.valueOf(publishedRowSystemId * PUBLISHING_PRIMARY_CODE_MULTIPLIER);
    }

    /**
     * Проверка на совпадение списка конфликтов.
     *
     * @param expectedList ожидаемый список
     * @param actualList   актуальный список
     */
    private void assertConflictEntities(List<RefBookConflictEntity> expectedList, List<RefBookConflictEntity> actualList) {
        assertNotNull(actualList);
        assertNotNull(expectedList);
        assertEquals(expectedList.size(), actualList.size());

        expectedList.forEach(expectedConflict -> {
            if (actualList.stream()
                    .noneMatch(actualConflict ->
                            expectedConflict.getReferrerVersion().equals(actualConflict.getReferrerVersion())
                                    && expectedConflict.getPublishedVersion().equals(actualConflict.getPublishedVersion())
                                    && expectedConflict.getRefFieldCode().equals(actualConflict.getRefFieldCode())
                                    && expectedConflict.getConflictType().equals(actualConflict.getConflictType())
                    ))
                fail();
        });
    }
}