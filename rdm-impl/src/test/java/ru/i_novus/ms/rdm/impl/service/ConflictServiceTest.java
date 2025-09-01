package ru.i_novus.ms.rdm.impl.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.*;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
    private static final String PUBLISHED_ATTRIBUTE_TEXT = "text";
    private static final String PUBLISHED_ATTRIBUTE_NAME_VALUE_SUFFIX = "_name";
    private static final String PUBLISHED_ATTRIBUTE_AMOUNT_VALUE_SUFFIX = "01";

    private static final Integer REFERRER_DRAFT_ID = -5;
    private static final String REFERRER_DRAFT_STORAGE_CODE = "TEST_REFERRER_STORAGE";

    private static final Integer PUBLISHING_DRAFT_ID = -6;
    private static final String PUBLISHING_DRAFT_STORAGE_CODE = "TEST_PUBLISHING_STORAGE";

    // for `testCalculateDamagedConflicts` && `testCalculateAlteredConflicts`:
    private static final String REFERRER_ATTRIBUTE_DELETE_REFERENCE = "delete_ref";
    private static final String REFERRER_REFERENCE_DELETE_EXPRESSION = "${name}: ${delete}";
    private static final String REFERRER_ATTRIBUTE_UPDATE_TYPE_REFERENCE = "update_type_ref";
    private static final String REFERRER_REFERENCE_UPDATE_TYPE_EXPRESSION = "${code} -- ${update_type}";
    private static final String REFERRER_ATTRIBUTE_UPDATE_CODE_REFERENCE = "update_code_ref";
    private static final String REFERRER_REFERENCE_UPDATE_CODE_EXPRESSION = "${updating_code} (${text})";

    private static final String PUBLISHED_ATTRIBUTE_INSERT = "insert";
    private static final String PUBLISHED_ATTRIBUTE_DELETE = "delete";
    private static final String PUBLISHED_ATTRIBUTE_UPDATE_NAME = "update_name";
    private static final String PUBLISHED_ATTRIBUTE_UPDATE_TYPE = "update_type";
    private static final String PUBLISHED_ATTRIBUTE_UPDATE_CODE_OLD = "updating_code";
    private static final String PUBLISHED_ATTRIBUTE_UPDATE_CODE_NEW = "updated_code";

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
    private SearchDataService searchDataService;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookConflictRepository conflictRepository;

    private RefBookVersionEntity referrerVersion;
    private RefBookVersionEntity publishedVersion;

    @Before
    public void setUp() {

        referrerVersion = createReferrerEntity(REFERRER_VERSION_ID, createReferrerStructure());
        referrerVersion.setStatus(RefBookVersionStatus.PUBLISHED);

        publishedVersion = createPublishingEntity(PUBLISHED_VERSION_ID, createPublishingStructure());
        publishedVersion.setStatus(RefBookVersionStatus.PUBLISHED);
    }

    @Test
    public void testCalculateDamagedConflicts() {

        referrerVersion.setStructure(createCalculateStructureConflictsReferrerStructure());
        publishedVersion.setStructure(createCalculateStructureConflictsVersionOldStructure());
        RefBookVersionEntity publishingEntity = createPublishingEntity(PUBLISHING_DRAFT_ID, createCalculateStructureConflictsVersionNewStructure());

        StructureDiff structureDiff = getCalculateStructureConflictsStructureDiff(publishedVersion.getStructure(), publishingEntity.getStructure());

        when(compareService.compareStructures(eq(publishedVersion.getId()), eq(publishingEntity.getId()))).thenReturn(structureDiff);

        List<RefBookConflictEntity> expectedList = asList(
                new RefBookConflictEntity(referrerVersion, publishingEntity, null, REFERRER_ATTRIBUTE_DELETE_REFERENCE, ConflictType.DISPLAY_DAMAGED),
                new RefBookConflictEntity(referrerVersion, publishingEntity, null, REFERRER_ATTRIBUTE_UPDATE_CODE_REFERENCE, ConflictType.DISPLAY_DAMAGED)
        );

        List<Structure.Reference> referrerReferences = referrerVersion.getStructure().getRefCodeReferences(publishedVersion.getRefBook().getCode());
        List<RefBookConflictEntity> actualList = calculateDamagedConflicts(referrerVersion,
                publishedVersion, publishingEntity, referrerReferences);
        assertConflictEntities(expectedList, actualList);
    }

    /**
     * Вычисление конфликтов, связанных с изменением структуры.
     *
     * @param referrerVersionEntity версия, которая ссылается
     * @param oldVersionEntity      старая версия, на которую ссылаются
     * @param newVersionEntity      новая версия, на которую будут ссылаться
     * @param referrerReferences    ссылки версии, которая ссылается
     */
    private List<RefBookConflictEntity> calculateDamagedConflicts(RefBookVersionEntity referrerVersionEntity,
                                                                  RefBookVersionEntity oldVersionEntity,
                                                                  RefBookVersionEntity newVersionEntity,
                                                                  List<Structure.Reference> referrerReferences) {
        StructureDiff structureDiff = compareService.compareStructures(oldVersionEntity.getId(), newVersionEntity.getId());

        return conflictService.calculateDisplayDamagedConflicts(referrerVersionEntity, newVersionEntity, referrerReferences, structureDiff);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testCalculateAlteredConflicts() {

        referrerVersion.setStructure(createCalculateStructureConflictsReferrerStructure());
        publishedVersion.setStructure(createCalculateStructureConflictsVersionOldStructure());
        RefBookVersionEntity publishingEntity = createPublishingEntity(PUBLISHING_DRAFT_ID, createCalculateStructureConflictsVersionNewStructure());

        Page<RefBookRowValue> referrerRowValues = createCalculateStructureConflictsReferrerRowValues();
        when(versionService.search(eq(referrerVersion.getId()), any(SearchDataCriteria.class))).thenReturn(referrerRowValues);

        when(versionValidation.equalsPrimaries(anyList(), anyList())).thenReturn(true);

        DataPage<RowValue> publishingRowValues = createCalculateStructureConflictsPublishedRowValues();
        when(searchDataService.getPagedData(any(StorageDataCriteria.class))).thenReturn(publishingRowValues);

        List<RefBookConflictEntity> expectedList = new ArrayList<>(10 + 10);
        LongStream.range(1, 10).forEach(systemId -> {
            if (systemId % 2 == 0) {
                RefBookConflictEntity entity = new RefBookConflictEntity(referrerVersion,
                        publishingEntity, systemId, REFERRER_ATTRIBUTE_DELETE_REFERENCE, ConflictType.ALTERED);
                expectedList.add(entity);
            }

            if (systemId % 3 == 0) {
                RefBookConflictEntity entity = new RefBookConflictEntity(referrerVersion,
                        publishingEntity, systemId, REFERRER_ATTRIBUTE_UPDATE_TYPE_REFERENCE, ConflictType.ALTERED);
                expectedList.add(entity);
            }
        });

        List<Structure.Reference> referrerReferences = referrerVersion.getStructure().getRefCodeReferences(publishedVersion.getRefBook().getCode());
        List<RefBookConflictEntity> actualList = calculateAlteredConflicts(referrerVersion,
                publishedVersion, publishingEntity, referrerReferences);
        assertConflictEntities(expectedList, actualList);
    }

    /**
     * Вычисление конфликтов, связанных с изменением структуры.
     *
     * @param referrerVersionEntity версия, которая ссылается
     * @param oldVersionEntity      старая версия, на которую ссылаются
     * @param newVersionEntity      новая версия, на которую будут ссылаться
     * @param referrerReferences    ссылки версии, которая ссылается
     */
    private List<RefBookConflictEntity> calculateAlteredConflicts(RefBookVersionEntity referrerVersionEntity,
                                                                  RefBookVersionEntity oldVersionEntity,
                                                                  RefBookVersionEntity newVersionEntity,
                                                                  List<Structure.Reference> referrerReferences) {
        List<RefBookConflictEntity> list = new ArrayList<>();

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(ConflictServiceImpl.REF_BOOK_VERSION_DATA_PAGE_SIZE);

        Page<RefBookRowValue> rowValues = versionService.search(referrerVersionEntity.getId(), criteria);
        referrerReferences.forEach(referrerReference -> {
            List<RefBookConflictEntity> entities = conflictService.calculateAlteredConflicts(referrerVersionEntity,
                    oldVersionEntity, newVersionEntity, referrerReference, rowValues.getContent());
            list.addAll(entities);
        });

        return list;
    }

    private Structure createReferrerStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.buildPrimary(REFERRER_ATTRIBUTE_CODE, "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_ATTRIBUTE_REFERENCE, "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference(REFERRER_ATTRIBUTE_REFERENCE, PUBLISHED_REF_BOOK_CODE, REFERRER_REFERENCE_DISPLAY_EXPRESSION)
                )
        );
    }

    @SuppressWarnings("SameParameterValue")
    private RefBookVersionEntity createReferrerEntity(Integer versionId, Structure structure) {

        final RefBookEntity refBookEntity = new DefaultRefBookEntity();
        refBookEntity.setId(REFERRER_REF_BOOK_ID);
        refBookEntity.setCode(REFERRER_REF_BOOK_CODE);

        final RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(versionId);
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setStatus(RefBookVersionStatus.DRAFT);
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    private Structure createPublishingStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.buildPrimary(PUBLISHED_ATTRIBUTE_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build(PUBLISHED_ATTRIBUTE_NAME, "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build(PUBLISHED_ATTRIBUTE_AMOUNT, "Количество", FieldType.INTEGER, "количество единиц"),
                        Structure.Attribute.build(PUBLISHED_ATTRIBUTE_TEXT, "Текст", FieldType.STRING, "текстовое описание")
                ),
                emptyList()
        );
    }

    private RefBookVersionEntity createPublishingEntity(Integer versionId, Structure structure) {

        RefBookEntity refBookEntity = new DefaultRefBookEntity();
        refBookEntity.setId(PUBLISHED_REF_BOOK_ID);
        refBookEntity.setCode(PUBLISHED_REF_BOOK_CODE);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(versionId);
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setStatus(RefBookVersionStatus.DRAFT);
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    private Structure createCalculateStructureConflictsReferrerStructure() {

        Structure structure = createReferrerStructure();

        List<Structure.Attribute> attributes = new ArrayList<>(structure.getAttributes());
        attributes.addAll(asList(
                Structure.Attribute.build(REFERRER_ATTRIBUTE_DELETE_REFERENCE, "delete_reference", FieldType.REFERENCE, "ссылка на удаляемый атрибут"),
                Structure.Attribute.build(REFERRER_ATTRIBUTE_UPDATE_TYPE_REFERENCE, "update_type_reference", FieldType.REFERENCE, "ссылка атрибут c изменяемым типом"),
                Structure.Attribute.build(REFERRER_ATTRIBUTE_UPDATE_CODE_REFERENCE, "update_code_reference", FieldType.REFERENCE, "ссылка атрибут c изменяемым кодом")
        ));
        structure.setAttributes(attributes);

        List<Structure.Reference> references = new ArrayList<>(structure.getReferences());
        references.addAll(asList(
                new Structure.Reference(REFERRER_ATTRIBUTE_DELETE_REFERENCE, PUBLISHED_REF_BOOK_CODE, REFERRER_REFERENCE_DELETE_EXPRESSION),
                new Structure.Reference(REFERRER_ATTRIBUTE_UPDATE_TYPE_REFERENCE, PUBLISHED_REF_BOOK_CODE, REFERRER_REFERENCE_UPDATE_TYPE_EXPRESSION),
                new Structure.Reference(REFERRER_ATTRIBUTE_UPDATE_CODE_REFERENCE, PUBLISHED_REF_BOOK_CODE, REFERRER_REFERENCE_UPDATE_CODE_EXPRESSION)
        ));
        structure.setReferences(references);

        return structure;
    }

    private Structure createCalculateStructureConflictsVersionOldStructure() {

        Structure structure = createPublishingStructure();

        List<Structure.Attribute> attributes = new ArrayList<>(structure.getAttributes());
        attributes.addAll(asList(
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_DELETE, "удаление", FieldType.STRING, "удаляемый атрибут"),
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_UPDATE_NAME, "старое имя", FieldType.STRING, "атрибут с изменяемым именем"),
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_UPDATE_TYPE, "смена типа", FieldType.INTEGER, "атрибут с изменяемым типом"),
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_UPDATE_CODE_OLD, "смена кода", FieldType.STRING, "атрибут с изменяемым кодом")
        ));
        structure.setAttributes(attributes);

        return structure;
    }

    private Structure createCalculateStructureConflictsVersionNewStructure() {

        Structure structure = createPublishingStructure();

        List<Structure.Attribute> attributes = new ArrayList<>(structure.getAttributes());
        attributes.addAll(asList(
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_INSERT, "добавление", FieldType.STRING, "добавляемый атрибут"),
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_UPDATE_NAME, "новое имя", FieldType.STRING, "атрибут с изменяемым именем"),
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_UPDATE_TYPE, "смена типа", FieldType.STRING, "атрибут с изменяемым типом"),
                Structure.Attribute.build(PUBLISHED_ATTRIBUTE_UPDATE_CODE_NEW, "смена кода", FieldType.STRING, "атрибут с изменяемым кодом")
        ));
        structure.setAttributes(attributes);

        return structure;
    }

    private StructureDiff getCalculateStructureConflictsStructureDiff(Structure oldStructure, Structure newStructure) {

        List<StructureDiff.AttributeDiff> inserted = asList(
                new StructureDiff.AttributeDiff(null, newStructure.getAttribute(PUBLISHED_ATTRIBUTE_INSERT)),
                new StructureDiff.AttributeDiff(null, newStructure.getAttribute(PUBLISHED_ATTRIBUTE_UPDATE_CODE_NEW))
        );

        List<StructureDiff.AttributeDiff> updated = asList(
                new StructureDiff.AttributeDiff(oldStructure.getAttribute(PUBLISHED_ATTRIBUTE_UPDATE_NAME), newStructure.getAttribute(PUBLISHED_ATTRIBUTE_UPDATE_NAME)),
                new StructureDiff.AttributeDiff(oldStructure.getAttribute(PUBLISHED_ATTRIBUTE_UPDATE_TYPE), newStructure.getAttribute(PUBLISHED_ATTRIBUTE_UPDATE_TYPE))
        );

        List<StructureDiff.AttributeDiff> deleted = asList(
                new StructureDiff.AttributeDiff(oldStructure.getAttribute(PUBLISHED_ATTRIBUTE_DELETE), null),
                new StructureDiff.AttributeDiff(oldStructure.getAttribute(PUBLISHED_ATTRIBUTE_UPDATE_CODE_OLD), null)
        );

        return new StructureDiff(inserted, updated, deleted);
    }

    private Page<RefBookRowValue> createCalculateStructureConflictsReferrerRowValues() {

        List<RefBookRowValue> rowValues = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());

        LongStream.range(1, 10).forEach(systemId -> {
            LongRowValue longRowValue = new LongRowValue(systemId, asList(
                    new StringFieldValue(REFERRER_ATTRIBUTE_CODE, getCalculateStructureConflictsReferrerPrimaryValue(systemId)),
                    new ReferenceFieldValue(REFERRER_ATTRIBUTE_DELETE_REFERENCE, createCalculateStructureConflictsReferrerReference(systemId, REFERRER_REFERENCE_DELETE_EXPRESSION)),
                    new ReferenceFieldValue(REFERRER_ATTRIBUTE_UPDATE_TYPE_REFERENCE, createCalculateStructureConflictsReferrerReference(systemId, REFERRER_REFERENCE_UPDATE_TYPE_EXPRESSION))
            ));
            rowValues.add(new RefBookRowValue(longRowValue, referrerVersion.getId()));
        });

        return new PageImpl<>(rowValues, Pageable.unpaged(), rowValues.size());
    }

    @SuppressWarnings("rawtypes")
    private DataPage<RowValue> createCalculateStructureConflictsPublishedRowValues() {

        List<RowValue> rowValues = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());

        LongStream.range(1, 10).forEach(systemId -> {
            LongRowValue longRowValue = new LongRowValue(systemId, singletonList(
                    new StringFieldValue(PUBLISHED_ATTRIBUTE_CODE, getCalculateStructureConflictsPublishedPrimaryValue(systemId))
            ));
            rowValues.add(longRowValue);
        });

        return new DataPage<>(rowValues.size(), rowValues, null);
    }

    private String getCalculateStructureConflictsReferrerPrimaryValue(Long systemId) {
        final Long REFERRER_PRIMARY_CODE_MULTIPLIER = 10L;
        return String.valueOf(systemId * REFERRER_PRIMARY_CODE_MULTIPLIER);
    }

    private Reference createCalculateStructureConflictsReferrerReference(Long systemId, String expression) {

        String publishedRowCode = String.valueOf(systemId);

        String value = null;
        String displayValue = null;

        switch (expression) {
            case REFERRER_REFERENCE_DELETE_EXPRESSION:
                if (systemId % 2 == 0) {
                    value = publishedRowCode;
                    displayValue = getCalculateStructureConflictsReferrerDisplayValue(systemId, expression);
                }
                break;

            case REFERRER_REFERENCE_UPDATE_TYPE_EXPRESSION:
                if (systemId % 3 == 0) {
                    value = publishedRowCode;
                    displayValue = getCalculateStructureConflictsReferrerDisplayValue(systemId, expression);
                }
                break;

            default:
                break;
        }

        return new Reference(REFERRER_DRAFT_STORAGE_CODE,
                null,
                PUBLISHED_ATTRIBUTE_CODE,
                new DisplayExpression(expression),
                value,
                displayValue
        );
    }

    private String getCalculateStructureConflictsReferrerDisplayValue(Long systemId, String expression) {
        String publishedRowCode = String.valueOf(systemId);
        return publishedRowCode + " # " + publishedRowCode;
    }

    private String getCalculateStructureConflictsPublishedPrimaryValue(Long systemId) {
        return String.valueOf(systemId);
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

        RefBookVersionEntity publishingEntity = createPublishingEntity(PUBLISHING_DRAFT_ID, createPublishingStructure());

        Page<RefBookConflictEntity> conflicts = createRecalculateConflictsPage();

        Page<RefBookRowValue> refFromRowValues = createRecalculateConflictsRowValues();
        when(versionService.search(eq(referrerVersion.getId()), any())).thenReturn(refFromRowValues);

        RefBookDataDiff refBookDataDiff = createRecalculateConflictsDataDiff();
        when(compareService.compareData(any())).thenReturn(refBookDataDiff);

        // Проверка в случае изменения только данных.
        List<RefBookConflictEntity> expectedList = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.size());
        CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.forEach(systemId -> {
            ConflictType conflictType =
                    PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING.equals(systemId) ? ConflictType.DELETED : ConflictType.UPDATED;
            expectedList.add(
                    new RefBookConflictEntity(referrerVersion, publishingEntity, systemId, REFERRER_ATTRIBUTE_REFERENCE, conflictType)
            );
        });

        List<RefBookConflictEntity> actualList = conflictService.recalculateDataConflicts(referrerVersion,
                publishedVersion, publishingEntity, conflicts.getContent(), false);
        assertConflictEntities(expectedList, actualList);

        // Проверка в случае изменения данных и структуры.
        List<RefBookConflictEntity> expectedAlteredList = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.size());
        CONFLICTED_PUBLISHED_ROW_SYS_IDS_EXPECTED.forEach(systemId -> {
            ConflictType conflictType =
                    PUBLISHED_ROW_SYS_ID_DELETED_UNCHANGING.equals(systemId) ? ConflictType.DELETED : ConflictType.UPDATED;

            if (!ConflictType.UPDATED.equals(conflictType)) {
                expectedAlteredList.add(
                        new RefBookConflictEntity(referrerVersion, publishingEntity, systemId, REFERRER_ATTRIBUTE_REFERENCE, conflictType)
                );
            }
        });

        List<RefBookConflictEntity> actualAlteredList = conflictService.recalculateDataConflicts(referrerVersion,
                publishedVersion, publishingEntity, conflicts.getContent(), true);
        assertConflictEntities(expectedAlteredList, actualAlteredList);
    }
    
    private Page<RefBookConflictEntity> createRecalculateConflictsPage() {

        List<RefBookConflictEntity> conflicts = new ArrayList<>(CONFLICTED_PUBLISHED_ROW_SYS_IDS.size());

        CONFLICTED_PUBLISHED_ROW_SYS_IDS_UPDATED.forEach(systemId ->
                conflicts.add(
                        new RefBookConflictEntity(referrerVersion, publishedVersion,
                                systemId, REFERRER_ATTRIBUTE_REFERENCE, ConflictType.UPDATED)
                )
        );

        CONFLICTED_PUBLISHED_ROW_SYS_IDS_DELETED.forEach(systemId ->
                conflicts.add(
                        new RefBookConflictEntity(referrerVersion, publishedVersion,
                                systemId, REFERRER_ATTRIBUTE_REFERENCE, ConflictType.DELETED)
                )
        );

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
            rowValues.add(new RefBookRowValue(longRowValue, referrerVersion.getId()));
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
                            Objects.equals(expectedConflict.getReferrerVersion(), actualConflict.getReferrerVersion())
                                    && Objects.equals(expectedConflict.getPublishedVersion(), actualConflict.getPublishedVersion())
                                    && Objects.equals(expectedConflict.getRefRecordId(), actualConflict.getRefRecordId())
                                    && Objects.equals(expectedConflict.getRefFieldCode(), actualConflict.getRefFieldCode())
                                    && Objects.equals(expectedConflict.getConflictType(), actualConflict.getConflictType())
                    ))
                fail();
        });
    }
}