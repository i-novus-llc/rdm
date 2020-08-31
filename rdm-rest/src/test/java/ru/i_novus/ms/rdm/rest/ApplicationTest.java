package ru.i_novus.ms.rdm.rest;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.jaxrs.RestMessage;
import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.platform.test.autoconfigure.EnableEmbeddedPg;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.common.file.storage.api.FileStorage;
import ru.i_novus.ms.rdm.api.enumeration.*;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.conflict.*;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.field.CommonField;
import ru.i_novus.ms.rdm.api.model.refbook.*;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.version.*;
import ru.i_novus.ms.rdm.api.service.*;
import ru.i_novus.ms.rdm.api.util.FieldValueUtils;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.validation.ReferenceValueValidation;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.*;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.service.StorageCodeService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import javax.sql.DataSource;
import java.io.*;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.junit.Assert.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.parseLocalDate;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.parseLocalDateTime;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.fields;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.rowValue;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "cxf.jaxrs.client.classes-scan=true",
                "cxf.jaxrs.client.classes-scan-packages=ru.i_novus.ms.rdm.api.service",
                "cxf.jaxrs.client.address=http://localhost:${server.port}/rdm/api",
                "fileStorage.root=src/test/resources/rdm/temp",
                "i18n.global.enabled=false",
                "rdm.audit.disabledActions=all"
        })
@DefinePort
@EnableEmbeddedPg
@Import(BackendConfiguration.class)
public class ApplicationTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ApplicationTest.class);

    private static final int REF_BOOK_ID = 500;
    private static final int REMOVABLE_REF_BOOK_ID = 501;
    private static final String REMOVABLE_REF_BOOK_CODE = "A082";
    private static final String ALL_TYPES_REF_BOOK_CODE = "all_types_ref_book";

    private static final String COMPARABLE_REF_BOOK_CODE = "comparable_ref_book";

    private static final String CONFLICTS_REF_BOOK_CODE = "conflicts_ref_book";

    private static final String SEARCH_CODE_STR = "A078 ";
    private static final String SEARCH_BY_NAME_STR = "отличное от последней версии ";
    private static final String SEARCH_BY_NAME_STR_ASSERT_CODE = "Z001";
    private static final String PASSPORT_ATTRIBUTE_FULL_NAME = "TEST_fullName";
    private static final String PASSPORT_ATTRIBUTE_SHORT_NAME = "TEST_shortName";
    private static final String PASSPORT_ATTRIBUTE_ANNOTATION = "TEST_annotation";
    private static final String PASSPORT_ATTRIBUTE_GROUP = "TEST_group";

    // RefBook for publishing (201807051500_RDM-29).
    private static final int TEST_PUBLISHING_VERSION_ID = -2;
    private static final String TEST_PUBLISHING_BOOK_CODE = "test_ref_book_for_draft";

    // Published RefBook (201905161200_RDM-452).
    //private static final int TEST_REFERENCE_BOOK_ID = -12;
    private static final String TEST_REFERENCE_BOOK_CODE = "test_ref_book_for_published";

    private static final String DATE_STR = "01.01.2011";

    private static RefBookCreateRequest referredBookRequest;
    private static Structure.Attribute referredIdAttribute;

    private static RefBookCreateRequest refBookCreateRequest;
    private static RefBookUpdateRequest refBookUpdateRequest;
    private static Structure.Attribute idAttribute;
    private static Structure.Attribute createAttribute;
    private static Structure.Reference createReference;

    private static Structure.Attribute updateAttribute;
    private static Structure.Attribute deleteAttribute;
    private static List<RefBookVersion> versionList;

    @Autowired
    @Qualifier("refBookServiceJaxRsProxyClient")
    private RefBookService refBookService;

    @Autowired
    @Qualifier("versionServiceJaxRsProxyClient")
    private VersionService versionService;

    @Autowired
    @Qualifier("draftServiceJaxRsProxyClient")
    private DraftService draftService;

    @Autowired
    @Qualifier("publishServiceJaxRsProxyClient")
    private PublishService publishService;

    @Autowired
    @Qualifier("compareServiceJaxRsProxyClient")
    private CompareService compareService;

    @Autowired
    @Qualifier("conflictServiceJaxRsProxyClient")
    private ConflictService conflictService;

    @Autowired
    @Qualifier("referenceServiceJaxRsProxyClient")
    private ReferenceService referenceService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("fileStorageServiceJaxRsProxyClient")
    private FileStorageService fileStorageService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private StorageCodeService storageCodeService;

    @Autowired
    private DraftDataService draftDataService;

    @Autowired
    private SearchDataService searchDataService;

    @BeforeClass
    public static void initialize() {

        referredBookRequest = new RefBookCreateRequest();
        referredBookRequest.setCode("REF_801");

        referredIdAttribute = Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.STRING, "значение для привязки");

        refBookCreateRequest = new RefBookCreateRequest();
        refBookCreateRequest.setCode("T1");

        Map<String, String> createPassport = new HashMap<>();
        createPassport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "Справочник специальностей");
        createPassport.put(PASSPORT_ATTRIBUTE_ANNOTATION, "Аннотация для справочника специальностей");
        createPassport.put(PASSPORT_ATTRIBUTE_GROUP, "Группа неизменна");
        refBookCreateRequest.setPassport(createPassport);

        refBookUpdateRequest = new RefBookUpdateRequest();
        refBookUpdateRequest.setCode(refBookCreateRequest.getCode() + "_upd");
        Map<String, String> updatePassport = new HashMap<>(createPassport);
        //Добавление
        updatePassport.put(PASSPORT_ATTRIBUTE_SHORT_NAME, "СПРВЧНК СПЦЛНСТЙ  ");
        //Изменение
        updatePassport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "Справочник специальностей_upd");
        updatePassport.put(PASSPORT_ATTRIBUTE_ANNOTATION, null);
        refBookUpdateRequest.setPassport(updatePassport);
        refBookUpdateRequest.setComment("обновленное наполнение");

        idAttribute = Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, "уникальное значение");
        createAttribute = Structure.Attribute.build("name", "Наименование", FieldType.REFERENCE, "описание");
        createReference = new Structure.Reference(createAttribute.getCode(), referredBookRequest.getCode(), toPlaceholder(referredIdAttribute.getCode()));
        updateAttribute = Structure.Attribute.build(createAttribute.getCode(),
                createAttribute.getName() + "_upd", createAttribute.getType(), createAttribute.getDescription() + "_upd");
        deleteAttribute = Structure.Attribute.build("code", "Код", FieldType.STRING, "на удаление");

        RefBookVersion version0 = new RefBookVersion();
        version0.setRefBookId(REF_BOOK_ID);
        version0.setStatus(RefBookVersionStatus.DRAFT);

        RefBookVersion version1 = new RefBookVersion();
        version1.setRefBookId(REF_BOOK_ID);
        version1.setStatus(RefBookVersionStatus.PUBLISHED);
        version1.setVersion("3");

        RefBookVersion version2 = new RefBookVersion();
        version2.setRefBookId(REF_BOOK_ID);
        version2.setStatus(RefBookVersionStatus.PUBLISHED);
        version2.setVersion("2");

        RefBookVersion version3 = new RefBookVersion();
        version3.setRefBookId(REF_BOOK_ID);
        version3.setStatus(RefBookVersionStatus.PUBLISHED);
        version3.setVersion("1");

        versionList = asList(version0, version1, version2, version3);
    }

    @AfterClass
    public static void cleanTemp() {
        File file = new File("src/test/resources/rdm/temp");
        deleteFile(file);
    }

    private static void deleteFile(File file) {
        if (Objects.isNull(file) || !file.exists())
            return;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (!Objects.isNull(files)) {
                for (File f : files)
                    deleteFile(f);
            }
        }

        file.delete();
    }

    /**
     * Создание справочника.
     * В архив.
     * Изменение метеданных справочника
     * Добавление/изменение/удаление атрибута
     * Получение справоника по идентификатору версии.
     */

    @Test
    public void testLifecycle() {

        RefBook referredBook = refBookService.create(referredBookRequest);
        assertNotNull(referredBook.getId());
        assertNotNull(referredBook.getRefBookId());
        assertEquals(referredBookRequest.getCode(), referredBook.getCode());

        Draft referredDraft = draftService.getDraft(referredBook.getId());
        assertNotNull(referredDraft);
        assertNotNull(referredDraft.getStorageCode());

        CreateAttributeRequest createReferredAttribute = new CreateAttributeRequest(null, referredIdAttribute, null);
        draftService.createAttribute(referredDraft.getId(), createReferredAttribute);

        Row referredRow = new Row(new HashMap<>() {{ put(referredIdAttribute.getCode(), "1"); }});
        updateData(referredDraft.getId(), referredRow, null);
        publish(referredDraft.getId(), null, null, null, false);

        // создание справочника
        RefBook refBook = refBookService.create(refBookCreateRequest);
        assertNotNull(refBook.getId());
        assertNotNull(refBook.getRefBookId());
        assertEquals(refBookCreateRequest.getCode(), refBook.getCode());
        assertPassportEqual(refBookCreateRequest.getPassport(), refBook.getPassport());
        assertEquals(RefBookVersionStatus.DRAFT, refBook.getStatus());
        assertNull(refBook.getVersion());
        assertNull(refBook.getComment());
        assertTrue(refBook.getRemovable());
        assertFalse(refBook.getArchived());
        assertNull(refBook.getFromDate());
        // получение черновика
        Draft draft = draftService.getDraft(refBook.getId());
        assertNotNull(draft);
        assertNotNull(draft.getStorageCode());
        final Integer draftId = draft.getId();
        assertNotNull(draftId);

        // изменение метаданных справочника
        refBookUpdateRequest.setVersionId(refBook.getId());
        RefBook updatedRefBook = refBookService.update(refBookUpdateRequest);
        refBook.setCode(refBookUpdateRequest.getCode());
        Map<String, String> expectedAttributesAfterUpdate = new HashMap<>();
        expectedAttributesAfterUpdate.putAll(refBookCreateRequest.getPassport());
        expectedAttributesAfterUpdate.putAll(refBookUpdateRequest.getPassport());
        expectedAttributesAfterUpdate.entrySet().removeIf(e -> e.getValue() == null);
        refBook.setPassport(expectedAttributesAfterUpdate);
        refBook.setComment(refBookUpdateRequest.getComment());
        refBook.setComment(refBookUpdateRequest.getComment());
        assertRefBooksEqual(refBook, updatedRefBook);

        // добавление атрибута и проверка
        CreateAttributeRequest createAttributeRequest = new CreateAttributeRequest(null, idAttribute, null);
        draftService.createAttribute(draftId, createAttributeRequest);

        createAttributeRequest = new CreateAttributeRequest(null, createAttribute, createReference);
        draftService.createAttribute(draftId, createAttributeRequest);
        Structure structure = versionService.getStructure(draftId);
        assertEquals(2, structure.getAttributes().size());
        assertEquals(createAttribute, structure.getAttribute(createAttribute.getCode()));
        assertEquals(createReference, structure.getReference(createAttribute.getCode()));

        // изменение атрибута и проверка
        UpdateAttributeRequest updateAttributeRequest = new UpdateAttributeRequest(null, updateAttribute, createReference);
        draftService.updateAttribute(draftId, updateAttributeRequest);
        structure = versionService.getStructure(draftId);
        assertEquals(updateAttribute, structure.getAttribute(updateAttributeRequest.getCode()));
        assertEquals(createReference, structure.getReference(updateAttributeRequest.getCode()));

        // удаление атрибута и проверка
        createAttributeRequest.setAttribute(deleteAttribute);
        createAttributeRequest.setReference(new Structure.Reference(null, null, null));
        draftService.createAttribute(draftId, createAttributeRequest);

        DeleteAttributeRequest deleteAttributeRequest = new DeleteAttributeRequest(null, deleteAttribute.getCode());
        draftService.deleteAttribute(draftId, deleteAttributeRequest);
        structure = versionService.getStructure(draftId);
        assertEquals(2, structure.getAttributes().size());

        // в архив
        refBookService.toArchive(refBook.getRefBookId());

        // получение по идентификатору версии
        RefBook refBookById = refBookService.getByVersionId(refBook.getId());
        refBook.setArchived(Boolean.TRUE);
        refBook.setRemovable(Boolean.FALSE);
        assertRefBooksEqual(refBook, refBookById);

        // удаление
        refBookService.delete(REMOVABLE_REF_BOOK_ID);
        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setCode(REMOVABLE_REF_BOOK_CODE);
        Page<RefBook> refBooks = refBookService.search(criteria);
        assertEquals(0, refBooks.getTotalElements());
    }

    /**
     * Поиск по идентификатору справочника
     * Поиск по наименованию.
     * Поиск по коду.
     * Поиск по коду и наименованию
     * Поиск по статусу.
     * Поиск по дате последней публикации.
     */
    @Test
    public void testRefBookSearch() {

        // поиск по идентификатору справочника
        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setRefBookIds(singletonList(REF_BOOK_ID));
        Page<RefBook> search = refBookService.search(refBookCriteria);
        assertEquals(1, search.getTotalElements());

        // поиск по коду (по подстроке без учета регистра, крайние пробелы)
        RefBookCriteria codeCriteria = new RefBookCriteria();
        codeCriteria.setCode(SEARCH_CODE_STR);
        search = refBookService.search(codeCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> assertTrue(containsIgnoreCase(r.getCode(), codeCriteria.getCode().trim())));

        // поиск по атрибуту паспорта
        RefBookCriteria nameCriteria = new RefBookCriteria();
        Map<String, String> passportMap = new HashMap<>();
        passportMap.put(PASSPORT_ATTRIBUTE_FULL_NAME, SEARCH_BY_NAME_STR);
        nameCriteria.setPassport(passportMap);
        RefBook refBook = refBookService.create(new RefBookCreateRequest(SEARCH_BY_NAME_STR_ASSERT_CODE, null, passportMap));

        search = refBookService.search(nameCriteria);
        assertEquals(1, search.getTotalElements());
        assertPassportEqual(refBook.getPassport(), search.getContent().get(0).getPassport());

        // поиск по статусу 'Черновик'
        RefBookCriteria draftCriteria = new RefBookCriteria();
        draftCriteria.setHasDraft(true);
        search = refBookService.search(draftCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertEquals(RefBookVersionStatus.DRAFT, r.getStatus());
        });

        // поиск по статусу 'Архив'
        RefBookCriteria archivedCriteria = new RefBookCriteria();
        archivedCriteria.setIsArchived(true);
        search = refBookService.search(archivedCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertTrue(r.getArchived());
            assertFalse(r.getRemovable());
        });

        // поиск по статусу 'Опубликован'
        RefBookCriteria publishedCriteria = new RefBookCriteria();
        publishedCriteria.setHasPublished(true);
        search = refBookService.search(publishedCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertNotNull(r.getLastPublishedVersionFromDate());
            assertFalse(r.getRemovable());
        });

        // поиск по дате публикации (дата начала, дата окончания)
        LocalDateTime fromDateBegin = parseLocalDateTime("01.02.2018 00:00:00");
        LocalDateTime fromDateEnd = parseLocalDateTime("17.02.2018 00:00:00");
        RefBookCriteria fromDateCriteria = new RefBookCriteria();
        fromDateCriteria.setFromDateBegin(fromDateBegin);
        fromDateCriteria.setFromDateEnd(fromDateEnd);
        search = refBookService.search(fromDateCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertTrue(r.getLastPublishedVersionFromDate().equals(fromDateBegin)
                    || r.getLastPublishedVersionFromDate().isAfter(fromDateBegin));
            assertTrue(r.getLastPublishedVersionFromDate().equals(fromDateEnd)
                    || r.getLastPublishedVersionFromDate().isBefore(fromDateEnd));
        });

        // поиск по дате последней публикации (дата начала и дата окончания вне диапазона действия существующих записей)
        fromDateCriteria.setFromDateBegin(parseLocalDateTime("01.01.2013 00:00:00"));
        fromDateCriteria.setFromDateEnd(parseLocalDateTime("01.02.2013 00:00:00"));
        search = refBookService.search(fromDateCriteria);
        assertEquals(0, search.getTotalElements());

        // поиск по дате публикации (только дата начала)
        LocalDateTime onlyFromDateBegin = parseLocalDateTime("01.02.2018 00:00:00");
        RefBookCriteria onlyFromDateBeginCriteria = new RefBookCriteria();
        onlyFromDateBeginCriteria.setFromDateBegin(onlyFromDateBegin);
        search = refBookService.search(onlyFromDateBeginCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r ->
                assertTrue(r.getLastPublishedVersionFromDate().equals(onlyFromDateBegin)
                        || r.getLastPublishedVersionFromDate().isAfter(onlyFromDateBegin)));
    }

    /**
     * Получение списка версий справочника
     * Получение списка версий без черновика
     * Поиск по номеру версии
     */
    @Test
    public void testGetVersions() {

        VersionCriteria criteria = new VersionCriteria();
        criteria.setRefBookId(versionList.get(0).getRefBookId());
        Page<RefBookVersion> search = versionService.getVersions(criteria);

        assertEquals(versionList.size(), search.getTotalElements());
        for (int i = 0; i < versionList.size(); i++) {
            RefBookVersion actual = search.getContent().get(i);
            assertVersion(versionList.get(i), actual);
        }

        // поиск с исключанием справочников
        criteria.setExcludeDraft(Boolean.TRUE);
        search = versionService.getVersions(criteria);
        assertEquals(versionList.size() - 1, search.getTotalElements());

        // поиск по номеру версии
        criteria.setVersion(versionList.get(1).getVersion());
        search = versionService.getVersions(criteria);
        assertEquals(1, search.getTotalElements());
    }

    @Test
    public void testDraftCreate() {

        Structure structure = createStructure();
        Draft expected = draftService.create(new CreateDraftRequest(1, structure));

        Draft actual = draftService.getDraft(expected.getId());
        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    public void testDraftReferrerCreate() {

        Structure structure = createTestStructureWithReferenceType();
        structure.getAttributes().clear();
        failDraftReferrerCreate(structure, false, "reference.attribute.not.found");

        structure = createTestStructureWithReferenceType();
        structure.setReferences(null);
        failDraftReferrerCreate(structure, false, "attribute.reference.not.found");

        structure = createTestStructureWithReferenceType();
        structure.setAttributes(structure.getPrimary());
        failDraftReferrerCreate(structure, false, "reference.attribute.not.found");

        structure = createTestStructureWithReferenceType();
        structure.getAttribute(structure.getReferences().get(0).getAttribute()).setPrimary(Boolean.TRUE);
        failDraftReferrerCreate(structure, false, "reference.attribute.cannot.be.primary.key");

        structure = createTestStructureWithReferenceType();
        structure.clearPrimary();
        failDraftReferrerCreate(structure, true, "reference.requires.primary.key");
    }

    private void failDraftReferrerCreate(Structure structure, boolean asUploaded, String message) {
        try {
            CreateDraftRequest request = new CreateDraftRequest(1, structure);
            if (asUploaded)
                request.setReferrerValidationRequired(true);

            draftService.create(request);
            fail();

        } catch (RestException re) {
            assertEquals(message, getRestExceptionMessage(re));
        }
    }

    @Test
    public void testDraftRemove() {

        Structure structure = createStructure();
        Draft draft = draftService.create(new CreateDraftRequest(1, structure));

        draftService.remove(draft.getId());
        try{
            draftService.getDraft(draft.getId());

        } catch (RestException e) {
            assertEquals("draft.not.found", e.getMessage());
        }
    }

    @Test
    public void testVersionSearch() {
        Page<RefBookRowValue> rowValues = versionService.search(-1, new SearchDataCriteria());

        List<FieldValue> fieldValues = rowValues.getContent().get(0).getFieldValues();
        StringFieldValue name = new StringFieldValue("name", "name");
        IntegerFieldValue count = new IntegerFieldValue("count", 2);

        assertEquals(name.getValue(), fieldValues.get(0).getValue());
        assertEquals(count.getValue(), fieldValues.get(1).getValue());
    }

    @Test
    public void testPublishFirstDraft() {

        publish(TEST_PUBLISHING_VERSION_ID, "1.0", LocalDateTime.now(), null, false);
        Page<RefBookRowValue> rowValuesInVersion = versionService.search(TEST_PUBLISHING_BOOK_CODE, LocalDateTime.now(), new SearchDataCriteria());

        RefBook refBook = refBookService.getByVersionId(TEST_PUBLISHING_VERSION_ID);
        assertEquals(TEST_PUBLISHING_BOOK_CODE, refBook.getCode());

        List fieldValues = rowValuesInVersion.getContent().get(0).getFieldValues();
        FieldValue name = new StringFieldValue("name", "name");
        FieldValue count = new IntegerFieldValue("count", 2);

        assertEquals(fieldValues.get(0), name);
        assertEquals(fieldValues.get(1), count);

        try {
            versionService.search(TEST_PUBLISHING_BOOK_CODE, LocalDateTime.now().minusDays(1), new SearchDataCriteria());
            fail();

        } catch (RestException e) {
            assertEquals("version.actual.on.date.not.found", e.getMessage());
        }
    }

    /**
     * Проверка методов работы со строками черновика, содержащего простые типы атрибутов.
     *
     * * updateData для одной строки:
     * - добавление строки, передается новая строка (нет systemId). Строка сохраняется в хранилище для версии. Кол-во строк = 1
     * - изменение строки, передается измененная строка (есть systemId). Строка сохраняется в хранилище для версии. Кол-во строк = 1
     * - удаление строки, передается systemId. Кол-во строк = 0
     * - добавление двух строк. Строки сохраняются в хранилище. Кол-во строк = 2
     * - изменение несуществующей строки. Ожидается ошибка с кодом: ненайденная строка.
     * - удаление всех строк. Кол-во строк = 0
     * - добавление невалидной строки (неверные значения целочисленного и ссылочного полей). Ожидается ошибка с двумя кодами: неверный тип и неверная ссылка.
     * * updateData для списка строк:
     * - добавление двух строк одновременно. Строки сохраняются в хранилище. Кол-во строк = 2
     * - добавление третьей строки и изменение второй одновременно. Строки сохраняются в хранилище. Кол-во строк = 3
     * - удаление всех строк. Кол-во строк = 0
     * - изменение несуществующей строки. Ожидается ошибка с кодом: ненайденная строка.
     */
    @Test
    public void testUpdateDataWithSimpleTypesOnly() {

        final String REFBOOK_CODE = "update_rows";
        RefBook refBook = refBookService.create(createRefBookCreateRequest(REFBOOK_CODE));
        Integer versionId = refBook.getId();

        Structure structure = createTestStructureWithSimpleTypesOnly();
        structure.getAttributes().forEach(
                attribute -> draftService.createAttribute(versionId, new CreateAttributeRequest(null, attribute, null))
        );

        // после создания черновик пустой
        assertEquals(0, draftService.search(versionId, new SearchDataCriteria()).getContent().size());

        // создание строки
        Row row = createRowWithSimpleTypesOnly("string", BigInteger.valueOf(1L), DATE_STR, true, 1.1);
        updateData(versionId, row, null);

        Long systemId = 1L;

        row.setSystemId(systemId);
        row.getData().replace("reference", new Reference("2", "2"));
        RowValue expectedRowValue = rowValue(row, structure);

        Page<RefBookRowValue> actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertRows(fields(structure), singletonList(expectedRowValue), actualRowValues.getContent());

        // изменение строки
        row.getData().replace("string", "string1");
        row.getData().replace("boolean", false);
        row.getData().replace("float", 1.2);
        updateData(versionId, row, null);

        expectedRowValue = rowValue(row, structure);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertRows(fields(structure), singletonList(expectedRowValue), actualRowValues.getContent());

        // удаление строки
        deleteData(versionId, new Row(1L, emptyMap()), null);

        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(0, actualRowValues.getContent().size());

        // создание двух строк
        Row row1 = createRowWithSimpleTypesOnly("string1", BigInteger.valueOf(1L), null, null, null);
        Row row2 = createRowWithSimpleTypesOnly("string2", BigInteger.valueOf(2L), null, null, null);
        updateData(versionId, row1, null);
        updateData(versionId, row2, null);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(2, actualRowValues.getContent().size());

        systemId++; // for row1
        systemId++; // for row2

        // изменение несуществующей строки
        row2.setSystemId(-1L);
        try {
            updateData(versionId, row2, null);
            fail();
        } catch (RestException re) {
            assertEquals("row.not.found", getRestExceptionMessage(re));
        }

        // удаление всех строк
        deleteAllData(versionId, null);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(0, actualRowValues.getContent().size());

        // создание невалидной строки
        Row badRow = createRowWithSimpleTypesOnly("string", BigInteger.valueOf(1L), DATE_STR, true, 1.1);
        badRow.getData().replace("integer", "abc");
        try {
            updateData(versionId, badRow, null);
            fail();
        } catch (RestException re) {
            Assert.assertEquals(1, re.getErrors().stream()
                    .map(RestMessage.Error::getMessage)
                    .filter("validation.type.error"::equals).count());
        }

        // создание двух строк одновременно
        row1 = createRowWithSimpleTypesOnly("string11", BigInteger.valueOf(11L), null, null, null);
        row2 = createRowWithSimpleTypesOnly("string22", BigInteger.valueOf(22L), null, null, null);
        updateData(versionId, asList(row1, row2), null);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(2, actualRowValues.getContent().size());

        systemId++; // for row1
        row1.setSystemId(systemId);

        systemId++; // for row2
        row2.setSystemId(systemId);

        // создание третьей строки и изменение второй одновременно
        row2.getData().replace("float", 2.2);
        Row row3 = createRowWithSimpleTypesOnly("string33", BigInteger.valueOf(33L), null, null, null);
        updateData(versionId, asList(row2, row3), null);

        systemId++; // for row3
        row3.setSystemId(systemId);

        List<RowValue> expectedRowValues = asList(
                rowValue(row1, structure), rowValue(row2, structure), rowValue(row3, structure)
        );

        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(3, actualRowValues.getContent().size());

        assertRows(fields(structure), expectedRowValues, actualRowValues.getContent());

        // удаление всех строк
        deleteAllData(versionId, null);

        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(0, actualRowValues.getContent().size());

        // изменение несуществующей строки
        row3.setSystemId(-1L);
        try {
            updateData(versionId, row3, null);
            fail();
        } catch (RestException re) {
            Assert.assertEquals(1, re.getErrors().stream()
                    .map(RestMessage.Error::getMessage)
                    .filter("row.not.found"::equals).count());
        }
    }

    /**
     * Проверка методов работы со строками черновика, содержащего ссылочный тип атрибута.
     *
     * @see ApplicationTest#testUpdateDataWithSimpleTypesOnly
     */
    @Test
    public void testUpdateDataWithReferenceType() {

        final String REFBOOK_CODE = "update_rows_with_ref";
        RefBook refBook = refBookService.create(createRefBookCreateRequest(REFBOOK_CODE));
        Integer versionId = refBook.getId();

        Structure structure = createTestStructureWithReferenceType();
        structure.getAttributes().forEach(attribute ->
                draftService.createAttribute(versionId, 
                        new CreateAttributeRequest(null, attribute,
                                attribute.isReferenceType() ? structure.getReference("reference") : null)
                )
        );

        // после создания черновик пустой
        assertEquals(0, draftService.search(versionId, new SearchDataCriteria()).getContent().size());

        // создание строки
        Row row = createRowWithReferenceType(BigInteger.valueOf(1L), "string", "2");
        updateData(versionId, row, null);

        Long systemId = 1L;

        row.setSystemId(systemId);
        row.getData().replace("reference", new Reference("2", "2"));
        RowValue expectedRowValue = rowValue(row, structure);

        Page<RefBookRowValue> actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertRows(fields(structure), singletonList(expectedRowValue), actualRowValues.getContent());

        // изменение строки
        row.getData().replace("string", "string1");
        row.getData().replace("reference", "2");
        updateData(versionId, row, null);

        row.getData().replace("reference", new Reference("2", "2"));
        expectedRowValue = rowValue(row, structure);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertRows(fields(structure), singletonList(expectedRowValue), actualRowValues.getContent());

        // удаление строки
        deleteData(versionId, new Row(1L, emptyMap()), null);

        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(0, actualRowValues.getContent().size());

        // создание двух строк
        Row row1 = createRowWithReferenceType(BigInteger.valueOf(1L), "string1", null);
        Row row2 = createRowWithReferenceType(BigInteger.valueOf(2L), "string2", null);
        updateData(versionId, row1, null);
        updateData(versionId, row2, null);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(2, actualRowValues.getContent().size());

        systemId++; // for row1
        systemId++; // for row2

        // изменение несуществующей строки
        row2.setSystemId(-1L);
        try {
            updateData(versionId, row2, null);
            fail();
        } catch (RestException re) {
            assertEquals("validation.db.contains.pk.err", getRestExceptionMessage(re));
        }

        // удаление всех строк
        deleteAllData(versionId, null);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(0, actualRowValues.getContent().size());

        // создание двух строк одновременно
        row1 = createRowWithReferenceType(BigInteger.valueOf(11L), "string11", null);
        row2 = createRowWithReferenceType(BigInteger.valueOf(22L), "string22", null);
        updateData(versionId, asList(row1, row2), null);
        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(2, actualRowValues.getContent().size());

        systemId++; // for row1
        row1.setSystemId(systemId);

        systemId++; // for row2
        row2.setSystemId(systemId);

        // создание третьей строки и изменение второй одновременно
        row2.getData().replace("string", "string.2.2");
        Row row3 = createRowWithReferenceType(BigInteger.valueOf(33L), "string33", null);
        updateData(versionId, asList(row2, row3), null);

        systemId++; // for row3
        row3.setSystemId(systemId);

        row1.getData().replace("reference", new Reference(null, null));
        row2.getData().replace("reference", new Reference(null, null));
        row3.getData().replace("reference", new Reference(null, null));
        List<RowValue> expectedRowValues = asList(
                rowValue(row1, structure), rowValue(row2, structure), rowValue(row3, structure)
        );

        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(3, actualRowValues.getContent().size());

        assertRows(fields(structure), expectedRowValues, actualRowValues.getContent());

        // удаление всех строк
        deleteAllData(versionId, null);

        actualRowValues = draftService.search(versionId, new SearchDataCriteria());
        assertEquals(0, actualRowValues.getContent().size());

        // изменение несуществующей строки
        row3.setSystemId(-1L);
        row3.getData().replace("reference", null);
        try {
            updateData(versionId, row3, null);
            fail();
        } catch (RestException re) {
            Assert.assertEquals(1, re.getErrors().stream()
                    .map(RestMessage.Error::getMessage)
                    .filter("row.not.found"::equals).count());
        }
    }

    /*
    * Поиск строки в актуальной на дату версии по первичным ключам
    * */
    @Test
    public void testSearchRowsByKeys() {

        final String REFBOOK_CODE = "search_by_keys";
        final String OLD_FILE_NAME = "oldData.xlsx";

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute code = Structure.Attribute.buildPrimary("CODE", "code", FieldType.STRING, "code");
        Structure.Attribute common = Structure.Attribute.build("COMMON", "common", FieldType.STRING,"common");
        Structure.Attribute descr = Structure.Attribute.build("DESCR", "descr", FieldType.STRING, "descr");
        Structure.Attribute upd = Structure.Attribute.build("UPD", "upd", FieldType.STRING, "upd");
        Structure.Attribute type = Structure.Attribute.build("TYPE", "type", FieldType.STRING, "type");

        Structure structure = new Structure(asList(id, code, common, descr, upd, type), emptyList());
        RefBook refBook = refBookService.create(createRefBookCreateRequest(REFBOOK_CODE));
        Integer oldVersionId = refBook.getId();
        structure.getAttributes()
                .forEach(attribute ->
                        draftService.createAttribute(oldVersionId, new CreateAttributeRequest(null, attribute, null))
                );
        updateFromFile(oldVersionId, null, OLD_FILE_NAME, "testCompare/" + OLD_FILE_NAME);
        publish(oldVersionId, "1.0", LocalDateTime.now(), null, false);

        Map<String, Object> rowMap = new HashMap<>() {{
            put(id.getCode(), BigInteger.valueOf(1L));
            put(code.getCode(), "001");
            put(common.getCode(), "c1");
            put(descr.getCode(), "descr1");
            put(upd.getCode(), "u1");
            put(type.getCode(), "1");
        }};
        Set<List<AttributeFilter>> filters = new HashSet<>() {{
            add(asList(
                    new AttributeFilter(id.getCode(), (Serializable) rowMap.get(id.getCode()), id.getType()),
                    new AttributeFilter(code.getCode(), (Serializable) rowMap.get(code.getCode()), code.getType())
            ));
        }};

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setAttributeFilters(filters);

        Page<RefBookRowValue> actualRow = versionService.search(REFBOOK_CODE, LocalDateTime.now(), searchDataCriteria);
        assertEquals(1, actualRow.getContent().size());
        assertRows(fields(structure), singletonList(rowValue(new Row(rowMap), structure)), actualRow.getContent());
    }

    /*
     * Поиск строки в актуальной на дату версии ничего не возвращает при поиске по отсутвующим в версии первичным ключам
     * Добавить версию со строками 1 2 3
     * Опубликовать
     * Добавить версию со стркоа ми 2 3(изменить) 4
     * Опубликовать
     * Поиск строки 1 во второй опубликованной версии
     * */
    @Test
    public void testSearchRowsByNonExistingKeys() {

        final String REFBOOK_CODE = "A002";
        final String OLD_FILE_NAME = "oldData.xlsx";
        final String NEW_FILE_NAME = "newData.xlsx";

        LocalDateTime publishDate1 = LocalDateTime.now();
        LocalDateTime closeDate1 = LocalDateTime.from(publishDate1.plusYears(2));
        LocalDateTime publishDate2 = LocalDateTime.from(publishDate1.plusYears(3));
        LocalDateTime closeDate2 = LocalDateTime.from(publishDate1.plusYears(5));

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute code = Structure.Attribute.buildPrimary("CODE", "code", FieldType.STRING, "code");
        Structure.Attribute common = Structure.Attribute.build("COMMON", "common", FieldType.STRING,"common");
        Structure.Attribute descr = Structure.Attribute.build("DESCR", "descr", FieldType.STRING, "descr");
        Structure.Attribute name = Structure.Attribute.build("NAME", "name", FieldType.STRING, "name");
        Structure.Attribute upd = Structure.Attribute.build("UPD", "upd", FieldType.STRING, "upd");
        Structure.Attribute type = Structure.Attribute.build("TYPE", "type", FieldType.STRING, "type");

        Structure structure = new Structure(asList(id, code, common, descr, upd, type), emptyList());
        RefBook refBook = refBookService.create(createRefBookCreateRequest(REFBOOK_CODE));
        Integer oldVersionId = refBook.getId();
        structure.getAttributes()
                .forEach(attribute ->
                        draftService.createAttribute(oldVersionId, new CreateAttributeRequest(null, attribute, null))
                );
        updateFromFile(oldVersionId, null, OLD_FILE_NAME, "testCompare/" + OLD_FILE_NAME);
        publish(oldVersionId, "1.0", publishDate1, closeDate1, false);

        Integer newVersionId = draftService
                .create(new CreateDraftRequest(refBook.getRefBookId(), new Structure(asList(id, code, common, name, upd, type), emptyList())))
                .getId();
        updateFromFile(newVersionId, null, NEW_FILE_NAME, "testCompare/" + NEW_FILE_NAME);
        publish(newVersionId, "1.1", publishDate2, closeDate2, false);

        List<AttributeFilter> filters = asList(
                new AttributeFilter(id.getCode(), BigInteger.valueOf(1L), id.getType()),
                new AttributeFilter(code.getCode(), "001", code.getType())
        );

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.addAttributeFilterList(filters);

        Page<RefBookRowValue> actualRow = versionService.search(REFBOOK_CODE,
                LocalDateTime.from(publishDate1.plusYears(4)), searchDataCriteria);
        assertEquals(0, actualRow.getContent().size());
    }

    /**
     * Проверка загрузки данных из XSLX в черновик, содержащий простые типы атрибутов.
     *
     * - Создаём новый черновик с ссылкой на опубликованную версию.
     * - Обновляем его данные из файла.
     */
    @Test
    public void testDraftUpdateDataFromFileWithSimpleTypesOnly() {

        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceTypeAsString(structure);

        Draft draft = draftService.create(new CreateDraftRequest(1, structure));
        updateFromFile(draft.getId(), null, "update_testUpload.xlsx", "testUpload.xlsx");

        Row row = createRowWithSimpleTypesOnly("Иван", BigInteger.valueOf(4L), DATE_STR, true, 1.0);
        extendRowWithReferenceType(row, BigInteger.valueOf(1L), "2");
        List<RowValue> expected = singletonList(rowValue(row, structure));

        Page<RefBookRowValue> search = draftService.search(draft.getId(), new SearchDataCriteria());
        assertRows(fields(structure), expected, search.getContent());
    }

    /**
     * Проверка загрузки данных из XSLX в черновик, содержащий ссылочный тип атрибута.
     *
     * @see ApplicationTest#testDraftUpdateDataFromFileWithSimpleTypesOnly
     */
    @Test
    public void testDraftUpdateDataFromFileWithReferenceType() {

        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceType(structure);

        Draft draft = draftService.create(new CreateDraftRequest(1, structure));
        updateFromFile(draft.getId(), null, "update_testUpload.xlsx", "testUpload.xlsx");

        Row row = createRowWithSimpleTypesOnly("Иван", BigInteger.valueOf(4L), DATE_STR, true, 1.0);
        extendRowWithReferenceType(row, BigInteger.valueOf(1L), new Reference("2", "2"));
        List<RowValue> expected = singletonList(rowValue(row, structure));

        Page<RefBookRowValue> search = draftService.search(draft.getId(), new SearchDataCriteria());
        assertRows(fields(structure), expected, search.getContent());
    }

    /**
     * Создаем новый черновик с ссылкой на опубликованную версию
     * Обновляем его данные из файла, который содержит невалидную ссылку
     */
    @Test()
    public void testDraftUpdateDataFromFileWithInvalidReference() {

        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceType(structure);
        Draft draft = draftService.create(new CreateDraftRequest(1, structure));

        try {
            updateFromFile(draft.getId(), null, "update_testUploadInvalidReference.xlsx", "testUploadInvalidReference.xlsx");
            fail();

        } catch (RestException re) {
            assertEquals(ReferenceValueValidation.REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE, getRestExceptionMessage(re));
        }
    }

    @Test
    public void testDraftCreateFromFile() {

        List<FieldValue> expectedData = new ArrayList<>() {{
            add(new StringFieldValue("string", "Иван"));
            add(new StringFieldValue("reference", "2"));
            add(new StringFieldValue("float", "1.0"));
            add(new StringFieldValue("date", DATE_STR));
            add(new StringFieldValue("boolean", "TRUE"));
            add(new StringFieldValue("integer", "4"));
            add(new StringFieldValue("id", "1"));
        }};
        FileModel fileModel = createFileModel("create_testUpload.xlsx", "testUpload.xlsx");
        Draft expected = draftService.create(-3, fileModel);
        Draft actual = draftService.getDraft(expected.getId());

        assertEquals(expected, actual);

        Page<RefBookRowValue> search = draftService.search(expected.getId(), new SearchDataCriteria());
        List actualData = search.getContent().get(0).getFieldValues();

        assertEquals(expectedData, actualData);
    }

    /*
    * Создаётся черновик справочника, заполняется данными, публикуется.
    * Создаётся новый черновик из версии с указанием предыдущей версии.
    * Проверяется, что структура и данные совпадают с предыдущей версией.
    * */
    @Test
    public void testDraftCreateFromVersion() {

        RefBookCreateRequest createRequest = new RefBookCreateRequest();
        createRequest.setCode("testDraftCreateFromVersionCode");
        RefBook refBook = refBookService.create(createRequest);

        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceType(structure);
        Draft draft1 = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), structure));

        Row row1 = createRowWithSimpleTypesOnly("test1", BigInteger.valueOf(1L), "01.09.2014", true, 1.1);
        extendRowWithReferenceType(row1, BigInteger.valueOf(1L), new Reference("77", null));

        Row row2 = createRowWithSimpleTypesOnly("test2", BigInteger.valueOf(2L), "01.10.2014", false, 2.2);
        extendRowWithReferenceType(row2, BigInteger.valueOf(2L), null);

        List<RowValue> expectedRowValues2 = asList(rowValue(row1, structure), rowValue(row2, structure));
        draftDataService.addRows(draft1.getStorageCode(), expectedRowValues2);
        publish(draft1.getId(), null, null, null, false);

        try {
            draftService.createFromVersion(0);
            fail();
        } catch (Exception e) {
            assertEquals("version.not.found", e.getMessage());
        }

        Draft draft2 = draftService.createFromVersion(draft1.getId());

        Structure draft1Structure = versionService.getStructure(draft1.getId());
        Structure draft2Structure = versionService.getStructure(draft2.getId());
        Assert.assertTrue(draft1Structure.storageEquals(draft2Structure));

        List<Field> draft1Fields = fields(draft1Structure);
        List<RefBookRowValue> rowValues2 = draftService.search(draft2.getId(), new SearchDataCriteria()).getContent();
        assertRows(draft1Fields, expectedRowValues2, rowValues2);

        RefBookRowValue deletedRowValue = rowValues2.stream()
                .filter(rowValue -> BigInteger.valueOf(2L).equals(rowValue.getFieldValue("integer").getValue()))
                .findFirst().orElse(null);
        assertNotNull(deletedRowValue);
        DeleteDataRequest deleteDataRequest = new DeleteDataRequest(null, ConverterUtil.toRow(deletedRowValue));
        draftService.deleteData(draft2.getId(), deleteDataRequest);

        Row row3 = createRowWithSimpleTypesOnly("test3", BigInteger.valueOf(3L), "01.11.2014", false, 3.3);
        extendRowWithReferenceType(row3, BigInteger.valueOf(3L), null);
        List<RowValue> expectedRowValues3 = new ArrayList<>(2);
        expectedRowValues3.add(rowValue(row3, structure));

        draftDataService.addRows(draft2.getStorageCode(), expectedRowValues3);
        publish(draft2.getId(), null, null, null, false);

        expectedRowValues3.add(rowValue(row1, structure));

        Draft draft3 = draftService.createFromVersion(draft2.getId());

        List<RefBookRowValue> rowValues3 = draftService.search(draft3.getId(), new SearchDataCriteria()).getContent();
        assertRows(draft1Fields, expectedRowValues3, rowValues3);
    }

    @Test
    public void testSearchInDraft() {

        RefBookCreateRequest createRequest = new RefBookCreateRequest();
        createRequest.setCode("myTestCodeRefBook");
        Map<String, String> createPassport = new HashMap<>();
        createPassport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "Справочник для тестирования версий");
        createRequest.setPassport(createPassport);
        RefBook refBook = refBookService.create(createRequest);

        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceType(structure);
        Draft draft = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), structure));

        Row row1 = createRowWithSimpleTypesOnly("Первое тестовое наименование", BigInteger.valueOf(1L), "01.09.2014", true, 2.4);
        extendRowWithReferenceType(row1, BigInteger.valueOf(1L), new Reference("77", null));

        Row row2 = createRowWithSimpleTypesOnly("Второе тестовое наименование", BigInteger.valueOf(3L), "01.10.2014", false, 0.4);
        extendRowWithReferenceType(row2, BigInteger.valueOf(2L), null);

        List<RowValue> rowValues = asList(rowValue(row1, structure), rowValue(row2, structure));
        draftDataService.addRows(draft.getStorageCode(), rowValues);

        List<RowValue> expectedRowValues = singletonList(rowValues.get(0));

        List<Field> fields = fields(structure);

        structure.getAttributes().forEach(attribute -> {
            String fullTextSearchValue = attribute.isReferenceType()
                    ? ((Reference) row1.getData().get(attribute.getCode())).getValue()
                    : row1.getData().get(attribute.getCode()).toString();
            SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
            searchDataCriteria.setCommonFilter(fullTextSearchValue);

            Page<RefBookRowValue> actualPage = draftService.search(draft.getId(), searchDataCriteria);
            Assert.assertEquals("Full text search failed", 1, actualPage.getContent().size());
            assertRows(fields, expectedRowValues, actualPage.getContent());
        });

        List<AttributeFilter> attributeFilters = new ArrayList<>();
        structure.getAttributes().forEach(attribute -> {
            Serializable searchValue = FieldType.REFERENCE.equals(attribute.getType())
                    ? ((Reference) row1.getData().get(attribute.getCode())).getValue()
                    : (Serializable) row1.getData().get(attribute.getCode());
            attributeFilters.add(new AttributeFilter(attribute.getCode(), searchValue, attribute.getType()));
        });

        attributeFilters.forEach(attributeFilter -> {
            SearchDataCriteria criteria = new SearchDataCriteria();
            criteria.addAttributeFilterList(singletonList(attributeFilter));

            Page<RefBookRowValue> actualPage = draftService.search(draft.getId(), criteria);
            assertRows(fields, expectedRowValues, actualPage.getContent());
        });

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.addAttributeFilterList(attributeFilters);

        Page<RefBookRowValue> actualPage = draftService.search(draft.getId(), searchDataCriteria);
        assertRows(fields, expectedRowValues, actualPage.getContent());
    }

    @Test
    public void testCreateRequiredAttributeWithNotEmptyData() {

        CreateAttributeRequest request = new CreateAttributeRequest(null, idAttribute, null);
        try {
            draftService.createAttribute(-3, request);
            fail();
        } catch (Exception e) {
            assertEquals("validation.required.pk.err", e.getMessage());
        }
    }

    @Test
    public void testExportImportDraftFile() throws IOException {
        //создание справочника из файла
        RefBook refBook = refBookService.create(createRefBookCreateRequest("Z002"));
        FileModel fileModel = createFileModel("create_testUpload.xlsx", "testUpload.xlsx");
        Draft draft1 = draftService.create(refBook.getRefBookId(), fileModel);
        Page<RefBookRowValue> expectedPage = draftService.search(draft1.getId(), new SearchDataCriteria());

        //выгрузка файла
        ExportFile exportFile = draftService.getDraftFile(draft1.getId(), FileType.XLSX);
        ZipInputStream zis = new ZipInputStream(exportFile.getInputStream());
        ZipEntry zipEntry = zis.getNextEntry();
        while (!zipEntry.getName().toLowerCase().contains("xlsx") && zis.available() > 0) {
            zipEntry = zis.getNextEntry();
        }
        fileModel = fileStorageService.save(zis, zipEntry.getName());

        //создание нового черновика из выгруженного
        Draft draft2 = draftService.create(refBook.getRefBookId(), fileModel);
        Assert.assertNotEquals(draft1, draft2);
        Page<RefBookRowValue> actualPage = draftService.search(draft2.getId(), new SearchDataCriteria());

        //сравнение двух черновиков
        Assert.assertEquals(expectedPage, actualPage);
    }

    @Test
    public void testValidatePrimaryKeyOnUpdateAttribute() {
        RefBookCreateRequest createRequest = new RefBookCreateRequest();
        createRequest.setCode("testValidatePrimaryKeyOnUpdateAttribute");
        Map<String, String> createPassport = new HashMap<>();
        createPassport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "Справочник для тестирования проверки изменения первичного ключа");
        createRequest.setPassport(createPassport);

        RefBook refBook = refBookService.create(createRequest);
        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, null),
                        Structure.Attribute.build("name", "Наименование", FieldType.STRING, null),
                        Structure.Attribute.build("code", "Код", FieldType.STRING, null)),
                null);
        Draft draft = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), structure));
        assertNotNull(draft);
        final Integer draftId = draft.getId();
        assertNotNull(draftId);

        List<String> codes = StructureUtils.getAttributeCodes(structure).collect(toList());
        Map<String, Object> rowMap1 = new HashMap<>();
        rowMap1.put(codes.get(0), BigInteger.valueOf(1L));
        rowMap1.put(codes.get(1), "Дублирующееся имя");
        rowMap1.put(codes.get(2), "001");

        Map<String, Object> rowMap2 = new HashMap<>();
        rowMap2.put(codes.get(0), BigInteger.valueOf(2L));
        rowMap2.put(codes.get(1), "Дублирующееся имя");
        rowMap2.put(codes.get(2), "0021");

        List<RowValue> rowValues = asList(
                rowValue(new Row(rowMap1), structure),
                rowValue(new Row(rowMap2), structure));

        draftDataService.addRows(draft.getStorageCode(), rowValues);

        structure.getAttribute("code").setPrimary(Boolean.TRUE);
        UpdateAttributeRequest updateAttributeRequest = new UpdateAttributeRequest(null, structure.getAttribute("name"), new Structure.Reference());
        draftService.updateAttribute(draftId, updateAttributeRequest);

        structure.getAttribute("name").setPrimary(Boolean.TRUE);
        updateAttributeRequest = new UpdateAttributeRequest(null, structure.getAttribute("name"), new Structure.Reference());
        try {
            draftService.updateAttribute(draftId, updateAttributeRequest);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof RestException);
            assertEquals("primary.key.not.unique", getRestExceptionMessage((RestException) e));
        }
    }

    /**
     * Тест на изменение структуры черновика без данных
     * <p>
     * Создаем новый черновик с ссылкой на опубликованную версию
     * Обновляем тип атрибута с любого на любой
     * Обновление без ошибок, так как в версии нет данных
     */
    @Test
    public void testUpdateAttributeTypeWithoutData() {

        RefBookCreateRequest refBookCreate = createRefBookCreateRequest(ALL_TYPES_REF_BOOK_CODE + "_wtht_data");
        RefBook refBook = refBookService.create(refBookCreate);
        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceType(structure);
        Structure.Reference reference = structure.getReference("reference");

        Draft draft = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), structure));

        // string -> integer, boolean, reference, float и обратно. Без ошибок
        reference.setAttribute("string");
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.INTEGER, null);
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.STRING, null);
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.BOOLEAN, null);
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.STRING, null);
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.REFERENCE, reference);
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.STRING, null);
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.FLOAT, null);
        validateUpdateTypeWithoutException(draft.getId(), "string", structure, FieldType.STRING, null);

        // integer -> string, boolean, reference, float и обратно. Без ошибок
        reference.setAttribute("integer");
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.STRING, null);
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.INTEGER, null);
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.BOOLEAN, null);
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.INTEGER, null);
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.REFERENCE, reference);
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.INTEGER, null);
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.FLOAT, null);
        validateUpdateTypeWithoutException(draft.getId(), "integer", structure, FieldType.INTEGER, null);

        // boolean -> string, integer, reference, float и обратно. Без ошибок
        reference.setAttribute("boolean");
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.STRING, null);
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.BOOLEAN, null);
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.INTEGER, null);
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.BOOLEAN, null);
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.REFERENCE, reference);
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.BOOLEAN, null);
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.FLOAT, null);
        validateUpdateTypeWithoutException(draft.getId(), "boolean", structure, FieldType.BOOLEAN, null);

        // reference -> string, integer, boolean, float и обратно. Без ошибок
        reference.setAttribute("reference");
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.STRING, null);
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.REFERENCE, reference);
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.INTEGER, null);
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.REFERENCE, reference);
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.BOOLEAN, null);
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.REFERENCE, reference);
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.FLOAT, null);
        validateUpdateTypeWithoutException(draft.getId(), "reference", structure, FieldType.REFERENCE, reference);

        // float -> string, integer, boolean, reference и обратно. Без ошибок
        reference.setAttribute("float");
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.STRING, null);
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.FLOAT, null);
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.INTEGER, null);
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.FLOAT, null);
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.BOOLEAN, null);
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.FLOAT, null);
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.REFERENCE, reference);
        validateUpdateTypeWithoutException(draft.getId(), "float", structure, FieldType.FLOAT, null);
    }

    /**
     * Тест на изменение структуры черновика с данными
     * <p>
     * Создаем новый черновик с ссылкой на опубликованную версию
     * Добавляем в версию наполнение
     * Пытаемся изменить тип атрибута с любого на любой
     * Без ошибок изменяется только тип поля string -> любой -> string. Возвращаются данные измененного типа
     * В остальных случаях ожидается ошибка
     */
    @Test
    public void testUpdateAttributeTypeWithData() {

        RefBookCreateRequest refBookCreate = createRefBookCreateRequest(ALL_TYPES_REF_BOOK_CODE + "_with_data");
        RefBook refBook = refBookService.create(refBookCreate);

        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceType(structure);
        Structure.Reference reference = structure.getReference("reference");

        Draft draft = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), structure));
        Integer draftId = draft.getId();
        updateFromFile(draftId, null, "update_testUpdateStr.xlsx", "testUpload.xlsx");

        // string -> integer, boolean, reference, float и обратно. Ожидается ошибка, так как данные неприводимы к другому типу
        reference.setAttribute("string");
        validateUpdateTypeWithException(draftId, "string", FieldType.STRING, FieldType.INTEGER, null);
        validateUpdateTypeWithException(draftId, "string", FieldType.STRING, FieldType.REFERENCE, reference);
        validateUpdateTypeWithException(draftId, "string", FieldType.STRING, FieldType.BOOLEAN, null);
        validateUpdateTypeWithException(draftId, "string", FieldType.STRING, FieldType.FLOAT, null);

        //float -> reference, date, boolean, integer. Ожидается ошибка (несовместимые типы)
        reference.setAttribute("float");
        validateUpdateTypeWithException(draftId, "float", FieldType.FLOAT, FieldType.REFERENCE, reference);
        validateUpdateTypeWithException(draftId, "float", FieldType.FLOAT, FieldType.DATE, null);
        validateUpdateTypeWithException(draftId, "float", FieldType.FLOAT, FieldType.BOOLEAN, null);
        validateUpdateTypeWithException(draftId, "float", FieldType.FLOAT, FieldType.INTEGER, null);

        //reference -> float, date, boolean, integer. Ожидается ошибка (несовместимые типы)
        validateUpdateTypeWithException(draftId, "reference", FieldType.REFERENCE, FieldType.FLOAT, null);
        validateUpdateTypeWithException(draftId, "reference", FieldType.REFERENCE, FieldType.DATE, null);
        validateUpdateTypeWithException(draftId, "reference", FieldType.REFERENCE, FieldType.BOOLEAN, null);
        validateUpdateTypeWithException(draftId, "reference", FieldType.REFERENCE, FieldType.INTEGER, null);

        //date -> float, reference, boolean, integer. Ожидается ошибка (несовместимые типы)
        reference.setAttribute("date");
        validateUpdateTypeWithException(draftId, "date", FieldType.DATE, FieldType.FLOAT, null);
        validateUpdateTypeWithException(draftId, "date", FieldType.DATE, FieldType.REFERENCE, reference);
        validateUpdateTypeWithException(draftId, "date", FieldType.DATE, FieldType.BOOLEAN, null);
        validateUpdateTypeWithException(draftId, "date", FieldType.DATE, FieldType.INTEGER, null);

        //boolean -> float, reference, date, integer. Ожидается ошибка (несовместимые типы)
        reference.setAttribute("boolean");
        validateUpdateTypeWithException(draftId, "boolean", FieldType.BOOLEAN, FieldType.FLOAT, null);
        validateUpdateTypeWithException(draftId, "boolean", FieldType.BOOLEAN, FieldType.REFERENCE, reference);
        validateUpdateTypeWithException(draftId, "boolean", FieldType.BOOLEAN, FieldType.DATE, null);
        validateUpdateTypeWithException(draftId, "boolean", FieldType.BOOLEAN, FieldType.INTEGER, null);

        //integer -> float, reference, date, boolean. Ожидается ошибка (несовместимые типы)
        reference.setAttribute("integer");
        validateUpdateTypeWithException(draftId, "integer", FieldType.INTEGER, FieldType.FLOAT, null);
        validateUpdateTypeWithException(draftId, "integer", FieldType.INTEGER, FieldType.REFERENCE, reference);
        validateUpdateTypeWithException(draftId, "integer", FieldType.INTEGER, FieldType.DATE, null);
        validateUpdateTypeWithException(draftId, "integer", FieldType.INTEGER, FieldType.BOOLEAN, null);

        // Все типы в string и обратно. Без ошибок
        List<RefBookRowValue> rowValues;
        // integer -> string -> integer
        structure.getAttribute("integer").setType(FieldType.STRING);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("integer"), null));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("integer") instanceof StringFieldValue);

        structure.getAttribute("integer").setType(FieldType.INTEGER);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("integer"), null));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("integer") instanceof IntegerFieldValue);

        // boolean -> string -> boolean
        structure.getAttribute("boolean").setType(FieldType.STRING);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("boolean"), null));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("boolean") instanceof StringFieldValue);

        structure.getAttribute("boolean").setType(FieldType.BOOLEAN);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("boolean"), null));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("boolean") instanceof BooleanFieldValue);

        reference.setAttribute("reference");
        // reference -> string -> reference
        structure.getAttribute("reference").setType(FieldType.STRING);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("reference"), null));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("reference") instanceof StringFieldValue);

        structure.getAttribute("reference").setType(FieldType.REFERENCE);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("reference"), reference));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("reference") instanceof ReferenceFieldValue);

        // float -> string -> float
        structure.getAttribute("float").setType(FieldType.STRING);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("float"), null));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("float") instanceof StringFieldValue);

        structure.getAttribute("float").setType(FieldType.FLOAT);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(null, structure.getAttribute("float"), reference));
        rowValues = versionService.search(draftId, new SearchDataCriteria()).getContent();
        assertTrue(rowValues.get(0).getFieldValue("float") instanceof FloatFieldValue);
    }

    /**
     * Тест на изменение optLockValue при изменении структуры.
     * <p>
     * - Создание нового черновика
     * - Проверка создания атрибута
     * - Проверка изменения атрибута
     * - Проверка удаления атрибута
     * - Проверка создания строки
     * - Проверка изменения строки
     * - Проверка удаления строки
     * - Проверка загрузки данных
     * <p>
     * Во всех случаях меняется optLockValue.
     */
    @Test
    public void testChangeOptLockValue() {

        RefBookCreateRequest refBookCreate = createRefBookCreateRequest("testOptLockValue");
        RefBook refBook = refBookService.create(refBookCreate);

        Structure structure = createTestStructureWithSimpleTypesOnly();
        extendTestStructureForReferenceType(structure);
        
        Draft draft = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), structure));
        Integer draftId = draft.getId();

        Draft actualDraft = draftService.getDraft(draftId);
        assertEquals(Integer.valueOf(0), actualDraft.getOptLockValue());

        // Создание атрибута.
        draft = actualDraft;
        String optAttributeCode = "opt_attr";
        Structure.Attribute optAttribute = Structure.Attribute.build(optAttributeCode, "optAttr", FieldType.INTEGER, "opt-значение");
        draftService.createAttribute(draftId, new CreateAttributeRequest(draft.getOptLockValue(), optAttribute, null));

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());

        // Изменение атрибута.
        draft = actualDraft;
        structure = versionService.getStructure(draftId);
        structure.getAttribute(optAttributeCode).setType(FieldType.STRING);
        draftService.updateAttribute(draftId, new UpdateAttributeRequest(draft.getOptLockValue(), structure.getAttribute("float"), null));

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());

        // Удаление атрибута.
        draft = actualDraft;
        draftService.deleteAttribute(draftId, new DeleteAttributeRequest(draft.getOptLockValue(), optAttributeCode));

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());

        // Создание строки.
        draft = actualDraft;
        Row row = createRowWithSimpleTypesOnly("string", BigInteger.valueOf(1), DATE_STR, true, 1.1);
        extendRowWithReferenceType(row, BigInteger.valueOf(1L), null);
        updateData(draftId, row, draft.getOptLockValue());

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());

        // Изменение строки.
        draft = actualDraft;
        Long systemId = 1L;
        row.setSystemId(systemId);
        row.getData().replace("string", "string1");
        row.getData().replace("boolean", false);
        row.getData().replace("float", 1.2);
        updateData(draftId, row, draft.getOptLockValue());

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());

        // Удаление строки.
        draft = actualDraft;
        deleteData(draftId, new Row(systemId, emptyMap()), draft.getOptLockValue());

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());

        // Загрузка данных.
        updateFromFile(draftId, null, "update_testOptLockValue.xlsx", "testUpload.xlsx");

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());

        // Обновление ссылок.
        draft = actualDraft;
        referenceService.refreshReferrer(draftId, draft.getOptLockValue());

        actualDraft = draftService.getDraft(draftId);
        assertNotEquals(draft.getOptLockValue(), actualDraft.getOptLockValue());
    }

    /*
    * currently system allows creating exactly one PK field
    * */
    @Test
    public void testUpdateFromFileValidation() {

        final String RELATION_REFBOOK_CODE = "Z003";
        final String RELATION_FILENAME = "Z003.xlsx";
        final String REFBOOK_CODE = "Z004";
        final String REFBOOK_FILENAME = "Z004.xlsx";
        final String REFBOOK_FILENAME_1 = "Z004_1.xlsx";
        final String RELATION_ATTR_CODE = "rel_string";
        final String PK_STRING = "pks";
        final String PK_REFERENCE = "pkr";
        final String PK_FLOAT = "pkf";
        final String PK_DATE = "pkd";
        final String PK_BOOL = "pkb";
        final String PK_INTEGER = "pki";
        final String NOT_PK_STRING = "npks";
        final String NOT_PK_REFERENCE = "npkr";
        final String NOT_PK_FLOAT = "npkf";
        final String NOT_PK_DATE = "npkd";
        final String NOT_PK_BOOL = "npkb";
        final String NOT_PK_INTEGER = "npki";

        //create new refbook
        RefBook relRefBook = refBookService.create(createRefBookCreateRequest(RELATION_REFBOOK_CODE));
        draftService.createAttribute(relRefBook.getId(), new CreateAttributeRequest(null, Structure.Attribute.buildPrimary(RELATION_ATTR_CODE, "string", FieldType.STRING, "string"), null));
        updateFromFile(relRefBook.getId(), null, RELATION_FILENAME, RELATION_FILENAME);
        publish(relRefBook.getId(), "1.0", LocalDateTime.now(), null, false);

        //create new refbook
        RefBook refBook = refBookService.create(createRefBookCreateRequest(REFBOOK_CODE));
        final Integer versionId = refBook.getId();

        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.buildPrimary(PK_STRING, PK_STRING, FieldType.STRING, "string"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.build(PK_REFERENCE, PK_REFERENCE, FieldType.REFERENCE, "count"),
                new Structure.Reference(PK_REFERENCE, RELATION_REFBOOK_CODE, toPlaceholder(RELATION_ATTR_CODE))
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.buildPrimary(PK_FLOAT, PK_FLOAT, FieldType.FLOAT, "float"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.buildPrimary(PK_DATE, PK_DATE, FieldType.DATE, "date"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.buildPrimary(PK_BOOL, PK_BOOL, FieldType.BOOLEAN, "boolean"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.buildPrimary(PK_INTEGER, PK_INTEGER, FieldType.INTEGER, "integer"), null
        ));

        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.build(NOT_PK_STRING, NOT_PK_STRING, FieldType.STRING, "string"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.build(NOT_PK_REFERENCE, NOT_PK_REFERENCE, FieldType.REFERENCE, "count"),
                new Structure.Reference(NOT_PK_REFERENCE, RELATION_REFBOOK_CODE, toPlaceholder(RELATION_ATTR_CODE))
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.build(NOT_PK_FLOAT, NOT_PK_FLOAT, FieldType.FLOAT, "float"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.build(NOT_PK_DATE, NOT_PK_DATE, FieldType.DATE, "date"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.build(NOT_PK_BOOL, NOT_PK_BOOL, FieldType.BOOLEAN, "boolean"), null
        ));
        draftService.createAttribute(versionId, new CreateAttributeRequest(null,
                Structure.Attribute.build(NOT_PK_INTEGER, NOT_PK_INTEGER, FieldType.INTEGER, "integer"), null
        ));

        updateFromFile(versionId, null, REFBOOK_FILENAME_1, REFBOOK_FILENAME_1);
        try {
            updateFromFile(versionId, null, REFBOOK_FILENAME, REFBOOK_FILENAME);
            fail();

        } catch (RestException re) {
            Assert.assertEquals(10, re.getErrors().size());
            Assert.assertEquals(1, re.getErrors().stream().map(RestMessage.Error::getMessage)
                    .filter("validation.db.contains.pk.err"::equals).count());
            Assert.assertEquals(1, re.getErrors().stream().map(RestMessage.Error::getMessage)
                    .filter("validation.required.pk.err"::equals).count());
            Assert.assertEquals(4, re.getErrors().stream().map(RestMessage.Error::getMessage)
                    .filter("validation.type.error"::equals).count());
            Assert.assertEquals(2, re.getErrors().stream().map(RestMessage.Error::getMessage)
                    .filter(ReferenceValueValidation.REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE::equals).count());
            Assert.assertEquals(2, re.getErrors().stream().map(RestMessage.Error::getMessage)
                    .filter("validation.not.unique.pk.err"::equals).count());
        }
    }

    /**
     * Тест публикации версии со всеми случаями пересечения с предыдущими (включая разные комбинации пересечения данных)<br/>
     */
    @Test
    public void testPublicationAllCases() {
        final String REF_BOOK_CODE = "testPublishAllCases";
        final String FIELD_NAME = "testFieldString";
        final String LEFT_FILE = "testPublishLeftIntersection.xlsx";
        final String MID_FILE = "testPublishMiddleIntersection.xlsx";
        final String RIGHT_FILE = "testPublishRightIntersection.xlsx";
        final String ALL_DATA = "testPublishAllDataIntersection.xlsx";
        final String NO_DATA = "testPublishNoDataIntersection.xlsx";

        List<RowValue> expectedLeft = createOneStringFieldRow(FIELD_NAME, "a", "b", "c", "f");
        List<RowValue> expectedMid = createOneStringFieldRow(FIELD_NAME, "b", "c", "d", "g");
        List<RowValue> expectedRight = createOneStringFieldRow(FIELD_NAME, "a", "b", "d", "e");
        List<RowValue> expectedAllData = createOneStringFieldRow(FIELD_NAME, "a", "b", "c", "d", "e", "f", "g");
        List<RowValue> expectedNoData = createOneStringFieldRow(FIELD_NAME, "h");

        RefBook refBook = refBookService.create(createRefBookCreateRequest(REF_BOOK_CODE));

        //Наличие черновика после создания справочника
        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setCode(REF_BOOK_CODE);
        criteria.setSourceType(RefBookSourceType.DRAFT);
        criteria.setPageSize(1);
        Page<RefBook> refBookPage = refBookService.searchVersions(criteria);
        assertEquals(1L, refBookPage.getContent().size());

        //Публикация левой версии
        Integer leftId = draftService.create(refBook.getRefBookId(), createFileModel(LEFT_FILE, "testPublishing/" + LEFT_FILE)).getId();
        publish(leftId, null, parseLocalDateTime("01.02.2018 00:00:00"), null, false);

        List<RefBookRowValue> actual = versionService.search(leftId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedLeft, actual);

        //Публикация средней версии
        Integer midId = draftService.create(refBook.getRefBookId(), createFileModel(MID_FILE, "testPublishing/" + MID_FILE)).getId();
        publish(midId, null, parseLocalDateTime("05.02.2018 00:00:00"),null, false);

        actual = versionService.search(leftId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedLeft, actual);
        actual = versionService.search(midId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedMid, actual);

        //Публикация правой версии
        Integer rightId = draftService.create(refBook.getRefBookId(), createFileModel(RIGHT_FILE, "testPublishing/" + RIGHT_FILE)).getId();
        publish(rightId, null, parseLocalDateTime("11.02.2018 00:00:00"), null, false);

        actual = versionService.search(leftId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedLeft, actual);
        actual = versionService.search(midId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedMid, actual);
        actual = versionService.search(rightId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedRight, actual);

        //Перекрывание конца левой и целиком средней версии новой, содержащей все прошлые данные
        //Ожидается:
        //Левая - правая гранится сместится влево до левой границы новой
        //Средняя - удалится
        //Правая - останется неизменной
        Integer allDataId = draftService.create(refBook.getRefBookId(), createFileModel(ALL_DATA, "testPublishing/" + ALL_DATA)).getId();
        publish(allDataId, null, parseLocalDateTime("02.02.2018 00:00:00"), parseLocalDateTime("10.02.2018 00:00:00"), false);

        actual = versionService.search(leftId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedLeft, actual);
        try {
            versionService.search(midId, new SearchDataCriteria()).getContent();
            fail();
        } catch (RestException e) {
            assertEquals("version.not.found", e.getMessage());
        }
        actual = versionService.search(rightId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedRight, actual);
        actual = versionService.search(allDataId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedAllData, actual);

        //Перекрывание предыдущей версии новой, не содержащей предыдущие данные
        //Ожидается: последняя версия удалится
        Integer noDataId = draftService.create(refBook.getRefBookId(), createFileModel(NO_DATA, "testPublishing/" + NO_DATA)).getId();
        publish(noDataId, null, parseLocalDateTime("02.02.2018 00:00:00"), parseLocalDateTime("10.02.2018 00:00:00"), false);

        actual = versionService.search(leftId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedLeft, actual);
        try {
            versionService.search(midId, new SearchDataCriteria()).getContent();
            fail();
        } catch (RestException e) {
            assertEquals("version.not.found", e.getMessage());
        }
        actual = versionService.search(rightId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedRight, actual);
        try {
            versionService.search(allDataId, new SearchDataCriteria()).getContent();
            fail();
        } catch (RestException e) {
            assertEquals("version.not.found", e.getMessage());
        }
        actual = versionService.search(noDataId, new SearchDataCriteria()).getContent();
        assertEqualRow(expectedNoData, actual);
    }

    /**
     * Тест публикации исходного справочника при наличии конфликтов в связанном справочнике.
     *
     * 1. Загрузка, проверка и публикация исходного справочника.
     * 2. Загрузка и проверка связанного справочника.
     * 3. Изменение исходного справочника и его публикация без обновления ссылок.
     * 4. Проверка связанного справочника и проверка конфликтов.
     * 5. Создание черновика связанного справочника.
     *
     * cardinalData.xml - исходный справочник для публикации после изменений в нём
     * referrerData.xml - связанный справочник с конфликтными ссылками на изменённый справочник
     */
    @Test
    public void testPublishWithConflictedReferrer() {

        final String REFERRER_FILE_NAME = "referrerData.xml";
        final String CARDINAL_FILE_NAME = "cardinalData.xml";
        final String REF_BOOK_FILE_FOLDER = "testPublishing/withConflicted/";

        final String REFERRER_REF_BOOK_CODE = "PUBLISH_CONFLICTED_REFERRER";
        final String CARDINAL_REF_BOOK_CODE = "PUBLISH_CONFLICTED_CARDINAL";

        final int REFERRER_DATA_ROW_COUNT = 1 + 2 + 3 + 3 + 3;
        final int CARDINAL_DATA_ROW_COUNT = 4 + 3 + 3 + 3;

        final int REFERRER_REFERENCE_COUNT = 3;
        final int REFERRER_ATTRIBUTE_COUNT = REFERRER_REFERENCE_COUNT + 1; // + primary
        final int CARDINAL_ATTRIBUTE_COUNT = 3;

        final String REFERRER_PRIMARY_ATTRIBUTE_CODE = "REF_BASE";
        final String REFERRER_NUMBER_ATTRIBUTE_CODE = "REF_NUMB";
        final String REFERRER_STRING_ATTRIBUTE_CODE = "REF_CHAR";
        final String REFERRER_MADEOF_ATTRIBUTE_CODE = "REF_MADE";

        final String REFERRER_PRIMARY_VALUE_111 = "REF_111";
        final String REFERRER_PRIMARY_VALUE_4__ = "REF_4__";
        final String REFERRER_PRIMARY_VALUE_69_ = "REF_69_";

        final List<String> REFERRER_UNCHANGED_PRIMARIES = asList("REF_111", "REF_123");
        final List<String> REFERRER_UPDATED_NUMBER_PRIMARIES = asList("REF_4__", "REF_444", "REF_69_");
        final List<String> REFERRER_UPDATED_STRING_PRIMARIES = asList("REF_14_", "REF_444", "REF_169");
        final List<String> REFERRER_UPDATED_MADEOF_PRIMARIES = asList("REF_444", "REF__84");
        final List<String> REFERRER_DELETED_NUMBER_PRIMARIES = asList("REF_8__", "REF_888");
        final List<String> REFERRER_DELETED_STRING_PRIMARIES = asList("REF_18_", "REF_888", "REF_69_", "REF__84");
        final List<String> REFERRER_DELETED_MADEOF_PRIMARIES = asList("REF_888", "REF_169");

        final Map<String, List<String>> REFERRER_UPDATED_PRIMARIES = Map.of(
                REFERRER_NUMBER_ATTRIBUTE_CODE, REFERRER_UPDATED_NUMBER_PRIMARIES,
                REFERRER_STRING_ATTRIBUTE_CODE, REFERRER_UPDATED_STRING_PRIMARIES,
                REFERRER_MADEOF_ATTRIBUTE_CODE, REFERRER_UPDATED_MADEOF_PRIMARIES
        );

        final Map<String, List<String>> REFERRER_DELETED_PRIMARIES = Map.of(
                REFERRER_NUMBER_ATTRIBUTE_CODE, REFERRER_DELETED_NUMBER_PRIMARIES,
                REFERRER_STRING_ATTRIBUTE_CODE, REFERRER_DELETED_STRING_PRIMARIES,
                REFERRER_MADEOF_ATTRIBUTE_CODE, REFERRER_DELETED_MADEOF_PRIMARIES
        );

        final String CARDINAL_PRIMARY_ATTRIBUTE_CODE = "CAR_CODE";
        final String CARDINAL_NUMBER_ATTRIBUTE_CODE = "CAR_NUMB";
        final String CARDINAL_STRING_ATTRIBUTE_CODE = "CAR_CHAR";

        final String CARDINAL_PRIMARY_VALUE_1_UNCHANGED = "TEST_1";
        final String CARDINAL_PRIMARY_VALUE_2_UNCHANGED = "TEST_2";
        final String CARDINAL_PRIMARY_VALUE_3_UNCHANGED = "TEST_3";
        final String CARDINAL_PRIMARY_VALUE_4_UPDATED = "TEST_4";
        final String CARDINAL_PRIMARY_VALUE_5_UPDATED = "TEST_5";
        final String CARDINAL_PRIMARY_VALUE_6_UPDATED = "TEST_6";
        final String CARDINAL_PRIMARY_VALUE_7_DELETED = "TEST_7";
        final String CARDINAL_PRIMARY_VALUE_8_DELETED = "TEST_8";
        final String CARDINAL_PRIMARY_VALUE_9_DELETED = "TEST_9";
        final String CARDINAL_PRIMARY_VALUE_0_INSERTED = "TEST_0";

//      1. Исходный справочник.
        // Загрузка из файла.
        FileModel cardinalFileModel = createFileModel(CARDINAL_FILE_NAME, REF_BOOK_FILE_FOLDER + CARDINAL_FILE_NAME);
        assertNotNull(cardinalFileModel);
        Draft cardinalDraft = refBookService.create(cardinalFileModel);

        assertNotNull(cardinalDraft);
        RefBookVersion cardinalVersion = versionService.getById(cardinalDraft.getId());
        assertNotNull(cardinalVersion);
        assertEquals(CARDINAL_REF_BOOK_CODE, cardinalVersion.getCode());
        assertEquals(cardinalDraft.getId(), cardinalVersion.getId());
        // Проверка структуры.
        Structure cardinalStructure = cardinalVersion.getStructure();
        assertEquals(CARDINAL_ATTRIBUTE_COUNT, cardinalStructure.getAttributes().size());
        Structure.Attribute cardinalPrimary = cardinalStructure.getAttribute(CARDINAL_PRIMARY_ATTRIBUTE_CODE);
        assertNotNull(cardinalPrimary);
        // Наличие данных.
        List<RefBookRowValue> cardinalContent = getVersionAllRowContent(cardinalVersion, cardinalDraft);
        assertNotNull(cardinalContent);
        assertEquals(CARDINAL_DATA_ROW_COUNT, cardinalContent.size());

        // Проверка данных.
        RefBookRowValue cardinalRowValue = getVersionRowValue(cardinalVersion.getId(), cardinalPrimary, CARDINAL_PRIMARY_VALUE_1_UNCHANGED);
        assertNotNull(cardinalRowValue);
        cardinalRowValue = getVersionRowValue(cardinalVersion.getId(), cardinalPrimary, CARDINAL_PRIMARY_VALUE_0_INSERTED);
        assertNull(cardinalRowValue);

        // Публикация для возможности создания ссылок на него.
        publish(cardinalDraft.getId(), null, LocalDateTime.now().minus(1, ChronoUnit.HOURS), null, false);
        RefBookVersion publishedVersion = versionService.getLastPublishedVersion(cardinalVersion.getCode());
        assertNotNull(publishedVersion);

//      2. Связанный справочник.
        // Загрузка из файла.
        FileModel referrerFileModel = createFileModel(REFERRER_FILE_NAME, REF_BOOK_FILE_FOLDER + REFERRER_FILE_NAME);
        assertNotNull(referrerFileModel);
        Draft referrerDraft = refBookService.create(referrerFileModel);
        assertNotNull(referrerDraft);
        RefBookVersion referrerVersion = versionService.getById(referrerDraft.getId());
        assertNotNull(referrerVersion);
        assertEquals(REFERRER_REF_BOOK_CODE, referrerVersion.getCode());
        assertEquals(referrerDraft.getId(), referrerVersion.getId());
        // Проверка структуры.
        Structure referrerStructure = referrerVersion.getStructure();
        assertNotNull(referrerStructure);
        assertEquals(REFERRER_ATTRIBUTE_COUNT, referrerStructure.getAttributes().size());
        assertEquals(REFERRER_REFERENCE_COUNT, referrerStructure.getReferences().size());
        Structure.Attribute referrerPrimary = referrerStructure.getAttribute(REFERRER_PRIMARY_ATTRIBUTE_CODE);
        assertNotNull(referrerPrimary);
        // Наличие данных.
        List<RefBookRowValue> referrerContent = getVersionAllRowContent(referrerVersion, referrerDraft);
        assertNotNull(referrerContent);
        assertEquals(REFERRER_DATA_ROW_COUNT, referrerContent.size());

        // Проверка данных.
        Map<String, BigInteger> expectedNumberValues = new HashMap<>();
        Map<String, String> expectedStringValues = new HashMap<>();
        Map<String, String> expectedMadeofValues = new HashMap<>();

        REFERRER_UNCHANGED_PRIMARIES.forEach(primaryValue -> {
            RefBookRowValue referrerRowValue = getVersionRowValue(referrerVersion.getId(), referrerPrimary, primaryValue);
            String displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_NUMBER_ATTRIBUTE_CODE);
            BigInteger numberValue = new BigInteger(displayValue);
            expectedNumberValues.put(primaryValue, numberValue);
            displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_STRING_ATTRIBUTE_CODE);
            expectedStringValues.put(primaryValue, displayValue);
            displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_MADEOF_ATTRIBUTE_CODE);
            expectedMadeofValues.put(primaryValue, displayValue);
        });

        REFERRER_UPDATED_NUMBER_PRIMARIES.forEach(primaryValue -> {
            RefBookRowValue referrerRowValue = getVersionRowValue(referrerVersion.getId(), referrerPrimary, primaryValue);
            String displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_NUMBER_ATTRIBUTE_CODE);
            BigInteger numberValue = getPublishWithConflictedReferrerNewNumberValue(new BigInteger(displayValue));
            expectedNumberValues.put(primaryValue, numberValue);
        });

        REFERRER_UPDATED_STRING_PRIMARIES.forEach(primaryValue -> {
            RefBookRowValue referrerRowValue = getVersionRowValue(referrerVersion.getId(), referrerPrimary, primaryValue);
            String displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_STRING_ATTRIBUTE_CODE);
            String stringValue = getPublishWithConflictedReferrerNewStringValue(displayValue);
            expectedStringValues.put(primaryValue, stringValue);
        });

        REFERRER_UPDATED_MADEOF_PRIMARIES.forEach(primaryValue -> {
            RefBookRowValue referrerRowValue = getVersionRowValue(referrerVersion.getId(), referrerPrimary, primaryValue);
            String displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_MADEOF_ATTRIBUTE_CODE);
            expectedMadeofValues.put(primaryValue, displayValue);
        });

//      3. Изменение исходного справочника.
        Draft changingDraft = draftService.createFromVersion(publishedVersion.getId());
        assertNotNull(changingDraft);
        RefBookVersion changingVersion = versionService.getById(changingDraft.getId());
        assertNotNull(changingVersion);
        // Изменение данных.
        updatePublishWithConflictedReferrerNumberValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_4_UPDATED, CARDINAL_NUMBER_ATTRIBUTE_CODE);
        updatePublishWithConflictedReferrerNumberValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_5_UPDATED, CARDINAL_NUMBER_ATTRIBUTE_CODE);
        updatePublishWithConflictedReferrerNumberValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_6_UPDATED, CARDINAL_NUMBER_ATTRIBUTE_CODE);

        updatePublishWithConflictedReferrerStringValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_4_UPDATED, CARDINAL_STRING_ATTRIBUTE_CODE);
        updatePublishWithConflictedReferrerStringValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_5_UPDATED, CARDINAL_STRING_ATTRIBUTE_CODE);
        updatePublishWithConflictedReferrerStringValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_6_UPDATED, CARDINAL_STRING_ATTRIBUTE_CODE);

        deletePublishWithConflictedReferrerValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_7_DELETED);
        deletePublishWithConflictedReferrerValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_8_DELETED);
        deletePublishWithConflictedReferrerValue(changingDraft, cardinalPrimary, CARDINAL_PRIMARY_VALUE_9_DELETED);

        // NB: insert.

        // Публикация изменений без обновления ссылок.
        publish(changingDraft.getId(), null, LocalDateTime.now(), null, false);
        RefBookVersion changedVersion = versionService.getLastPublishedVersion(cardinalVersion.getCode());
        assertNotNull(changedVersion);

        // Обновление ссылок.
        referenceService.refreshLastReferrers(changedVersion.getCode());

//      4. Проверка связанного справочника.
        // Проверка данных.
        REFERRER_UNCHANGED_PRIMARIES.forEach(primaryValue -> {
            RefBookRowValue referrerRowValue = getVersionRowValue(referrerVersion.getId(), referrerPrimary, primaryValue);
            String displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_NUMBER_ATTRIBUTE_CODE);
            assertEquals(expectedNumberValues.get(primaryValue), new BigInteger(displayValue));
            displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_STRING_ATTRIBUTE_CODE);
            assertEquals(expectedStringValues.get(primaryValue), displayValue);
            displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, REFERRER_MADEOF_ATTRIBUTE_CODE);
            assertEquals(expectedMadeofValues.get(primaryValue), displayValue);
        });

        REFERRER_UPDATED_PRIMARIES.forEach((primaryField, primaryList) ->
            primaryList.forEach(primaryValue -> {
                RefBookRowValue referrerRowValue = getVersionRowValue(referrerVersion.getId(), referrerPrimary, primaryValue);
                String displayValue = getPublishWithConflictedReferrerDisplayValue(referrerRowValue, primaryField);
                switch(primaryField) {
                    case REFERRER_NUMBER_ATTRIBUTE_CODE:
                        assertEquals(expectedNumberValues.get(primaryValue), new BigInteger(displayValue));
                        break;

                    case REFERRER_STRING_ATTRIBUTE_CODE:
                        assertEquals(expectedStringValues.get(primaryValue), displayValue);
                        break;

                    case REFERRER_MADEOF_ATTRIBUTE_CODE:
                        assertNotEquals(expectedMadeofValues.get(primaryValue), displayValue);
                        break;

                    default:
                        break;
                }
            })
        );

        // Проверка конфликтов.
        REFERRER_UNCHANGED_PRIMARIES.forEach(primaryValue -> {
            RefBookConflict conflict = findPublishWithConflictedReferrerConflict(
                    referrerVersion.getId(), changedVersion.getId(),
                    referrerPrimary, primaryValue, REFERRER_NUMBER_ATTRIBUTE_CODE);
            assertNull(conflict);
        });

        REFERRER_UPDATED_PRIMARIES.forEach((primaryField, primaryList) ->
            primaryList.forEach(primaryValue -> {
                RefBookConflict conflict = findPublishWithConflictedReferrerConflict(
                        referrerVersion.getId(), changedVersion.getId(),
                        referrerPrimary, primaryValue, primaryField);
                assertNull(conflict);
            })
        );

        REFERRER_DELETED_PRIMARIES.forEach((primaryField, primaryList) ->
            primaryList.forEach(primaryValue -> {
                RefBookConflict conflict = findPublishWithConflictedReferrerConflict(
                        referrerVersion.getId(), changedVersion.getId(),
                        referrerPrimary, primaryValue, primaryField);
                assertNotNull(conflict);
            })
        );

//      5. Создание черновика связанного справочника.
        List<Long> systemIds = LongStream.range(1, 100).boxed().collect(toList());

        List<Long> referrerConflictedIds = conflictService.getReferrerConflictedIds(referrerVersion.getId(), systemIds);

        // Публикация связанного справочника.
        publish(referrerDraft.getId(), null, LocalDateTime.now(), null, false);
        RefBookVersion lastReferrerVersion = versionService.getLastPublishedVersion(referrerVersion.getCode());
        assertNotNull(lastReferrerVersion);

        conflictService.copyConflicts(referrerVersion.getId(), lastReferrerVersion.getId());

        // Создание черновика из версии.
        referrerDraft = draftService.createFromVersion(referrerVersion.getId());
        List<Long> lastReferrerConflictedIds = conflictService.getReferrerConflictedIds(referrerDraft.getId(), systemIds);
        assertEquals(referrerConflictedIds, lastReferrerConflictedIds);

    }

    private String getPublishWithConflictedReferrerDisplayValue(RefBookRowValue rowValue, String attributeCode) {
        assertNotNull(rowValue);
        FieldValue referrerFieldValue = rowValue.getFieldValue(attributeCode);
        assertTrue(referrerFieldValue instanceof ReferenceFieldValue);
        Reference referrerReference = ((ReferenceFieldValue)referrerFieldValue).getValue();
        assertNotNull(referrerReference);
        return referrerReference.getDisplayValue();
    }

    private BigInteger getPublishWithConflictedReferrerNewNumberValue(BigInteger value) {
        return value.multiply(BigInteger.valueOf(11L));
    }

    private String getPublishWithConflictedReferrerNewStringValue(String value) {
        return value + "___" + value;
    }

    @SuppressWarnings("SameParameterValue")
    private void updatePublishWithConflictedReferrerNumberValue(Draft draft,
                                                                Structure.Attribute primary, String primaryValue,
                                                                String fieldName) {
        RefBookRowValue rowValue = getVersionRowValue(draft.getId(), primary, primaryValue);
        assertNotNull(rowValue);
        assertNotNull(rowValue.getSystemId());

        FieldValue fieldValue = rowValue.getFieldValue(fieldName);
        assertTrue(fieldValue instanceof IntegerFieldValue);
        IntegerFieldValue typedFieldValue = (IntegerFieldValue)fieldValue;
        BigInteger newTypedValue = getPublishWithConflictedReferrerNewNumberValue(typedFieldValue.getValue());
        typedFieldValue.setValue(newTypedValue);

        draftDataService.updateRows(draft.getStorageCode(), singletonList(rowValue));

        rowValue = getVersionRowValue(draft.getId(), primary, primaryValue);
        assertNotNull(rowValue);
        assertNotNull(rowValue.getSystemId());

        fieldValue = rowValue.getFieldValue(fieldName);
        assertTrue(fieldValue instanceof IntegerFieldValue);
        typedFieldValue = (IntegerFieldValue)fieldValue;
        assertEquals(newTypedValue, typedFieldValue.getValue());
    }

    @SuppressWarnings("SameParameterValue")
    private void updatePublishWithConflictedReferrerStringValue(Draft draft,
                                                                Structure.Attribute primary, String primaryValue,
                                                                String fieldName) {
        RefBookRowValue rowValue = getVersionRowValue(draft.getId(), primary, primaryValue);
        assertNotNull(rowValue);
        assertNotNull(rowValue.getSystemId());

        FieldValue fieldValue = rowValue.getFieldValue(fieldName);
        assertTrue(fieldValue instanceof StringFieldValue);
        StringFieldValue typedFieldValue = (StringFieldValue)fieldValue;
        String newTypedValue = getPublishWithConflictedReferrerNewStringValue(typedFieldValue.getValue());
        typedFieldValue.setValue(newTypedValue);

        draftDataService.updateRows(draft.getStorageCode(), singletonList(rowValue));

        rowValue = getVersionRowValue(draft.getId(), primary, primaryValue);
        assertNotNull(rowValue);
        assertNotNull(rowValue.getSystemId());

        fieldValue = rowValue.getFieldValue(fieldName);
        assertTrue(fieldValue instanceof StringFieldValue);
        typedFieldValue = (StringFieldValue)fieldValue;
        assertEquals(newTypedValue, typedFieldValue.getValue());
    }

    private void deletePublishWithConflictedReferrerValue(Draft draft, Structure.Attribute primary, String primaryValue) {
        RefBookRowValue rowValue = getVersionRowValue(draft.getId(), primary, primaryValue);
        assertNotNull(rowValue);
        assertNotNull(rowValue.getSystemId());

        deleteData(draft.getId(), new Row(rowValue.getSystemId(), emptyMap()), null);

        rowValue = getVersionRowValue(draft.getId(), primary, primaryValue);
        assertNull(rowValue);
    }

    private RefBookConflict findPublishWithConflictedReferrerConflict(Integer referrerId, Integer cardinalId,
                                                                      Structure.Attribute primary, String primaryValue,
                                                                      String fieldName) {
        RefBookRowValue rowValue = getVersionRowValue(referrerId, primary, primaryValue);
        assertNotNull(rowValue);
        assertNotNull(rowValue.getSystemId());

        RefBookConflictCriteria criteria = new RefBookConflictCriteria(referrerId, cardinalId, fieldName, null);
        criteria.setRefRecordId(rowValue.getSystemId());
        criteria.setPageSize(1);
        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return (conflicts == null || isEmpty(conflicts.getContent())) ? null : conflicts.getContent().get(0);
    }

    @Test
    public void testToArchive() {
        RefBook refBook = refBookService.create(createRefBookCreateRequest("testArchive"));
        assertFalse(refBookService.getByVersionId(refBook.getId()).getArchived());

        refBookService.toArchive(refBook.getRefBookId());
        assertTrue(refBookService.getByVersionId(refBook.getId()).getArchived());

        refBookService.fromArchive(refBook.getRefBookId());
        assertFalse(refBookService.getByVersionId(refBook.getId()).getArchived());
    }

    @Test
    public void testSortInRefBookSearch() {

        final String REFBOOK_CODE = "refbookSortCode";
        Map<String, String> passport = new HashMap<>();

        passport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "order1");
        passport.put(PASSPORT_ATTRIBUTE_SHORT_NAME, "order3");
        RefBook refBook1 = refBookService.create(new RefBookCreateRequest(REFBOOK_CODE + 2, null, passport));

        passport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "order3");
        passport.put(PASSPORT_ATTRIBUTE_SHORT_NAME, "order2");
        RefBook refBook2 = refBookService.create(new RefBookCreateRequest(REFBOOK_CODE + 3, null, passport));

        passport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "order3");
        passport.put(PASSPORT_ATTRIBUTE_SHORT_NAME, "order1");
        RefBook refBook3 = refBookService.create(new RefBookCreateRequest(REFBOOK_CODE + 1, null, passport));

        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setCode(REFBOOK_CODE);
        Iterator<RefBook> expected1 = asList(
                refBook1,
                refBook2,
                refBook3).iterator();
        refBookService.search(criteria).getContent()
                .forEach(actual -> assertRefBooksEqual(expected1.next(), actual));

        criteria.setOrders(singletonList(new Sort.Order(Sort.Direction.ASC, "code")));
        Iterator<RefBook> expected2 = asList(
                refBook3,
                refBook1,
                refBook2).iterator();
        refBookService.search(criteria).getContent()
                .forEach(actual -> assertRefBooksEqual(expected2.next(), actual));

        criteria.setOrders(singletonList(new Sort.Order(Sort.Direction.DESC, "code")));
        Iterator<RefBook> expected3 = asList(
                refBook2,
                refBook1,
                refBook3).iterator();
        refBookService.search(criteria).getContent()
                .forEach(actual -> assertRefBooksEqual(expected3.next(), actual));

        criteria.setOrders(singletonList(new Sort.Order(Sort.Direction.ASC, "passport." + PASSPORT_ATTRIBUTE_SHORT_NAME)));
        Iterator<RefBook> expected4 = asList(
                refBook3,
                refBook2,
                refBook1).iterator();
        refBookService.search(criteria).getContent()
                .forEach(actual -> assertRefBooksEqual(expected4.next(), actual));

        criteria.setOrders(asList(
                new Sort.Order(Sort.Direction.ASC, "passport." + PASSPORT_ATTRIBUTE_FULL_NAME),
                new Sort.Order(Sort.Direction.ASC, "code")));
        Iterator<RefBook> expected5 = asList(
                refBook1,
                refBook3,
                refBook2).iterator();
        refBookService.search(criteria).getContent()
                .forEach(actual -> assertRefBooksEqual(expected5.next(), actual));
    }

    /*
    * compare data for two published versions with different storage codes
    * id - primary key (PK)
    * code - common non-primary field (no changes)
    * common - common non-primary field (UPDATED)
    * descr - field from OLD version (DELETED)
    * name - field from NEW version (INSERTED)
    * upd1, upd2 - field with updated name (UPDATED field)
    * typeS, typeI - field with updated type (UPDATED field)
    * */
    @Test
    public void testCompareVersionsData() {
        final String OLD_FILE_NAME = "oldData.xlsx";
        final String NEW_FILE_NAME = "newData.xlsx";

        LocalDateTime publishDate1 = LocalDateTime.now();
        LocalDateTime closeDate1 = publishDate1.plusYears(2);
        LocalDateTime publishDate2 = publishDate1.plusYears(3);
        LocalDateTime closeDate2 = publishDate1.plusYears(4);

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute code = Structure.Attribute.build("CODE", "code", FieldType.STRING, "code");
        Structure.Attribute common = Structure.Attribute.build("COMMON", "common", FieldType.STRING,"common");
        Structure.Attribute descr = Structure.Attribute.build("DESCR", "descr", FieldType.STRING, "descr");
        Structure.Attribute name = Structure.Attribute.build("NAME", "name", FieldType.STRING, "name");
        Structure.Attribute upd1 = Structure.Attribute.build("UPD", "upd1", FieldType.STRING, "upd");
        Structure.Attribute upd2 = Structure.Attribute.build("UPD", "upd2", FieldType.STRING, "upd");
        Structure.Attribute typeS = Structure.Attribute.build("TYPE", "type", FieldType.STRING, "type");
        Structure.Attribute typeI = Structure.Attribute.build("TYPE", "type", FieldType.INTEGER, "type");

        RefBook refBook = refBookService.create(createRefBookCreateRequest(COMPARABLE_REF_BOOK_CODE + "_diff"));
        Integer oldVersionId = refBook.getId();
        asList(id, code, common, descr, upd1, typeS)
                .forEach(attribute ->
                        draftService.createAttribute(oldVersionId, new CreateAttributeRequest(null, attribute, null))
                );
        updateFromFile(oldVersionId, null, OLD_FILE_NAME, "testCompare/" + OLD_FILE_NAME);
        publish(oldVersionId, "1.0", publishDate1, closeDate1, false);

        Integer newVersionId = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), new Structure(asList(id, code, common, name, upd2, typeI), emptyList()))).getId();
        updateFromFile(newVersionId, null, NEW_FILE_NAME, "testCompare/" + NEW_FILE_NAME);
        publish(newVersionId, "1.1", publishDate2, closeDate2, false);

        Field idField = new CommonField(id.getCode());
        Field codeField = new CommonField(code.getCode());
        Field commonField = new CommonField(common.getCode());

        List<DiffRowValue> expectedDiffRowValues = new ArrayList<>();
        expectedDiffRowValues.add(new DiffRowValue(
                asList(
                        new DiffFieldValue<>(idField, BigInteger.valueOf(1L), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(codeField, "001", null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(commonField, "c1", null, DiffStatusEnum.DELETED)),
                DiffStatusEnum.DELETED));
        expectedDiffRowValues.add(new DiffRowValue(
                asList(
                        new DiffFieldValue<>(idField, null,  BigInteger.valueOf(4L), DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(codeField, null, "004", DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(commonField, null,  "c4", DiffStatusEnum.INSERTED)),
                DiffStatusEnum.INSERTED));
        expectedDiffRowValues.add(new DiffRowValue(
                asList(
                        new DiffFieldValue<>(idField, null,  BigInteger.valueOf(3L), null),
                        new DiffFieldValue<>(codeField, null, "003", null),
                        new DiffFieldValue<>(commonField, "c3",  "c3_1", DiffStatusEnum.UPDATED)),
                DiffStatusEnum.UPDATED));

        RefBookDataDiff expectedRefBookDataDiff = new RefBookDataDiff(
                new PageImpl<>(expectedDiffRowValues, PageRequest.of(0, 10), expectedDiffRowValues.size()),
                singletonList(descr.getCode()),
                singletonList(name.getCode()),
                asList(upd1.getCode(), typeI.getCode())
        );

        RefBookDataDiff actualRefBookDataDiff = compareService.compareData(new CompareDataCriteria(oldVersionId, newVersionId));
        assertRefBookDataDiffs(expectedRefBookDataDiff, actualRefBookDataDiff);
    }

    /*
     * testing get data difference for two versions with no differences for common fields
     * empty refBookDataDiff is expected (with empty list)
     */
    @Test
    public void testCompareDataWhenNoDiff() {
        final String FILE_NAME = "testCompareData.xlsx";

        LocalDateTime publishDate1 = LocalDateTime.now();
        LocalDateTime publishDate2 = publishDate1.plusYears(2);

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute code = Structure.Attribute.build("CODE", "code", FieldType.STRING, "code");
        Structure.Attribute name = Structure.Attribute.build("NAME", "name", FieldType.STRING, "name");

        RefBook refBook = refBookService.create(createRefBookCreateRequest(COMPARABLE_REF_BOOK_CODE + "_no_diff"));
        Integer oldVersionId = refBook.getId();
        draftService.createAttribute(oldVersionId, new CreateAttributeRequest(null, id, null));
        draftService.createAttribute(oldVersionId, new CreateAttributeRequest(null, code, null));

        updateFromFile(refBook.getId(), null, FILE_NAME, "testCompare/" + FILE_NAME);
        publish(refBook.getId(), "1.0", publishDate1, null, false);

        Integer newVersionId = draftService.create(
                new CreateDraftRequest(
                        refBook.getRefBookId(),
                        new Structure(asList(
                                id,
                                code,
                                name),
                                emptyList())))
                .getId();
        updateFromFile(newVersionId, null, FILE_NAME, "testCompare/" + FILE_NAME);
        publish(newVersionId, "1.1", publishDate2, null, false);

        List<DiffRowValue> expectedDiffRowValues = new ArrayList<>();
        RefBookDataDiff expectedRefBookDataDiff = new RefBookDataDiff(
                new PageImpl<>(expectedDiffRowValues, PageRequest.of(0, 10), expectedDiffRowValues.size()),
                emptyList(),
                singletonList(name.getCode()),
                emptyList()
        );

        RefBookDataDiff actualRefBookDataDiff = compareService.compareData(new CompareDataCriteria(oldVersionId, newVersionId));
        assertRefBookDataDiffs(expectedRefBookDataDiff, actualRefBookDataDiff);
    }

    /*
     * testing get data difference for two versions with different primary fields
     * Exception is expected
     */
    @Test
    public void testCompareDataWhenDifferentPK() {
        final String FILE_NAME = "testCompareData.xlsx";

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute code = Structure.Attribute.build("CODE", "code", FieldType.STRING, "code");

        RefBook refBook = refBookService.create(createRefBookCreateRequest(COMPARABLE_REF_BOOK_CODE + "_diff_pk"));
        Integer oldVersionId = refBook.getId();
        draftService.createAttribute(oldVersionId, new CreateAttributeRequest(null, id, null));
        draftService.createAttribute(oldVersionId, new CreateAttributeRequest(null, code, null));

        updateFromFile(refBook.getId(), null, FILE_NAME, "testCompare/" + FILE_NAME);
        publish(refBook.getId(), "1.0", LocalDateTime.now(), null, false);

        Integer newVersionId = draftService.create(
                new CreateDraftRequest(
                        refBook.getRefBookId(),
                        new Structure(asList(
                                Structure.Attribute.build("ID", "id", FieldType.INTEGER, "id"),
                                Structure.Attribute.buildPrimary("CODE", "code", FieldType.STRING, "code")),
                                emptyList())))
                .getId();
        updateFromFile(newVersionId, null, FILE_NAME, "testCompare/" + FILE_NAME);
        publish(newVersionId, "1.1", LocalDateTime.now().plusYears(1), null, false);

        try {
            compareService.compareData(new CompareDataCriteria(oldVersionId, newVersionId));
            fail();
        } catch (RestException re) {
            assertEquals("compare.primaries.not.match", re.getMessage());
        }
    }

    /*
     * testing calculate conflicts for two refBooks
     * refFromRefBook has two links to a published version of refToRefBook (REF_ID_1, REF_ID_2)
     * refFromRefBook version has 5 rows:
     *   ref_1, ref_2
     * 1) DEL, UPD (2 conflicts for 1st row)
     * 2) -, - (no conflicts)
     * 3) DEL, - (1 conflict for 3rd row)
     * 4) -, UPD (1 conflict for 4th row)
     * 5) null, null (no conflicts)
     * create new draft version of refToRefBook with INSERTED (no matter), UPDATED (ref_id=3), DELETED (ref_id=1) rows
     *
     * calculate conflicts must return 4 conflicts
     * for DELETED and UPDATED rows
     * only for the rows, which were referenced by refFromRefBook
     */
    @Test
    public void testCalculateConflictWhenConflictsExist() {
        final String OLD_FILE_NAME = "oldRefToData.xlsx";
        final String NEW_FILE_NAME = "newRefToData.xlsx";
        final String REF_FILE_NAME = "refData.xlsx";

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute code = Structure.Attribute.build("CODE", "code", FieldType.STRING, "code");

        Structure structure = new Structure(asList(id, code), emptyList());

        RefBook refToRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_to_confl"));
        Integer refToVersionId = refToRefBook.getId();
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, id, null));
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, code, null));

        updateFromFile(refToVersionId, null, OLD_FILE_NAME, "testConflicts/" + OLD_FILE_NAME);
        publish(refToVersionId, "1.0", LocalDateTime.now(), null, false);

        Integer refToDraftId = draftService.create(
                new CreateDraftRequest(
                        refToRefBook.getRefBookId(),
                        structure))
                .getId();
        updateFromFile(refToDraftId, null, NEW_FILE_NAME, "testConflicts/" + NEW_FILE_NAME);

        Structure.Attribute id_id = Structure.Attribute.buildPrimary("ID_ID", "id_id", FieldType.INTEGER, "id_id");
        Structure.Attribute ref_id_1 = Structure.Attribute.build("REF_ID_1", "ref_id_1", FieldType.REFERENCE, "ref_id_1");
        Structure.Attribute ref_id_2 = Structure.Attribute.build("REF_ID_2", "ref_id_2", FieldType.REFERENCE, "ref_id_2");
        Structure.Reference ref_id_1_ref = new Structure.Reference(ref_id_1.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(id.getCode()));
        Structure.Reference ref_id_2_ref = new Structure.Reference(ref_id_2.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(id.getCode()));

        RefBook refFromRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_from_confl"));
        Integer refFromVersionId = refFromRefBook.getId();
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, id_id, null));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_id_1, ref_id_1_ref));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_id_2, ref_id_2_ref));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, code, null));

        updateFromFile(refFromVersionId, null, REF_FILE_NAME, "testConflicts/" + REF_FILE_NAME);
        publish(refFromVersionId, "1.0", LocalDateTime.now(), null, false);

        List<Conflict> expectedConflicts = asList(
                new Conflict(ref_id_1.getCode(), ConflictType.DELETED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(1L)))),
                new Conflict(ref_id_2.getCode(), ConflictType.UPDATED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(1L)))),
                new Conflict(ref_id_1.getCode(), ConflictType.DELETED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(3L)))),
                new Conflict(ref_id_2.getCode(), ConflictType.UPDATED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(4L))))
        );

        // NB: Use createCalculatedDataConflicts + load conflicts from DB.
        List<Conflict> actualConflicts = calculateDataConflicts(refFromVersionId, refToVersionId, refToDraftId);
        assertConflicts(expectedConflicts, actualConflicts);
    }

    @Test
    public void testCalculateConflictWhenStructureChanged() {
        final String OLD_FILE_NAME = "oldRefToData.xlsx";
        final String NEW_FILE_NAME = "newRefToData.xlsx";
        final String REF_FILE_NAME = "refData.xlsx";

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute fixedAttr = Structure.Attribute.build("FIX_ATTR", "fix-attr", FieldType.STRING, "fixed attribute");
        Structure.Attribute updatedAttr = Structure.Attribute.build("UPD_ATTR", "upd-attr", FieldType.STRING, "updated attribute");
        Structure.Attribute deletedAttr = Structure.Attribute.build("DEL_ATTR", "del-attr", FieldType.STRING, "deleted attribute");

        Structure structure = new Structure(asList(id, fixedAttr, updatedAttr, deletedAttr), emptyList());

        RefBook refToRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_to_struc"));
        Integer refToVersionId = refToRefBook.getId();
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, id, null));
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, fixedAttr, null));
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, updatedAttr, null));
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, deletedAttr, null));

        updateFromFile(refToVersionId, null, OLD_FILE_NAME, "testConflicts/structured/" + OLD_FILE_NAME);
        publish(refToVersionId, "1.0", LocalDateTime.now(), null, false);

        Integer refToDraftId = draftService.create(
                new CreateDraftRequest(
                        refToRefBook.getRefBookId(),
                        structure))
                .getId();
        updateFromFile(refToDraftId, null, NEW_FILE_NAME, "testConflicts/structured/" + NEW_FILE_NAME);

        Structure.Attribute id_id = Structure.Attribute.buildPrimary("ID_ID", "id_id", FieldType.INTEGER, "id_id");
        Structure.Attribute ref_fix = Structure.Attribute.build("REF_FIX", "ref_fix", FieldType.REFERENCE, "ref to fixed attr");
        Structure.Attribute ref_upd = Structure.Attribute.build("REF_UPD", "ref_upd", FieldType.REFERENCE, "ref to updated attr");
        Structure.Attribute ref_del = Structure.Attribute.build("REF_DEL", "ref_del", FieldType.REFERENCE, "ref to deleted attr");
        Structure.Reference ref_fix_ref = new Structure.Reference(ref_fix.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(fixedAttr.getCode()));
        Structure.Reference ref_upd_ref = new Structure.Reference(ref_upd.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(updatedAttr.getCode()));
        Structure.Reference ref_del_ref = new Structure.Reference(ref_del.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(deletedAttr.getCode()));

        RefBook refFromRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_from_struc"));
        Integer refFromVersionId = refFromRefBook.getId();
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, id_id, null));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_fix, ref_fix_ref));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_upd, ref_upd_ref));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_del, ref_del_ref));

        updateFromFile(refFromVersionId, null, REF_FILE_NAME, "testConflicts/structured/" + REF_FILE_NAME);
        publish(refFromVersionId, "1.0", LocalDateTime.now(), null, false);

        Structure.Attribute insertedAttribute = Structure.Attribute.build("INS_ATTR", "ins-attr", FieldType.INTEGER, "inserted attribute");
        draftService.createAttribute(refToDraftId, new CreateAttributeRequest(null, insertedAttribute, null));
        Structure.Attribute updatedAttribute = Structure.Attribute.build("UPD_ATTR", "upd-attr", FieldType.INTEGER, "updated attribute");
        draftService.updateAttribute(refToDraftId, new UpdateAttributeRequest(null, updatedAttribute, null));
        draftService.deleteAttribute(refToDraftId, new DeleteAttributeRequest(null, deletedAttr.getCode()));

        List<Conflict> expectedConflicts = asList(
                new Conflict(ref_fix.getCode(), ConflictType.DELETED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(1L)))),
                new Conflict(ref_fix.getCode(), ConflictType.DELETED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(3L)))),

                new Conflict(ref_upd.getCode(), ConflictType.UPDATED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(1L)))),
                new Conflict(ref_upd.getCode(), ConflictType.UPDATED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(4L)))),

                new Conflict(ref_del.getCode(), ConflictType.DELETED, singletonList(
                    new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(1L)))),
                new Conflict(ref_del.getCode(), ConflictType.UPDATED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(3L)))),
                new Conflict(ref_del.getCode(), ConflictType.UPDATED, singletonList(
                        new IntegerFieldValue(id_id.getCode(), BigInteger.valueOf(4L))))
        );

        // NB: Use createCalculatedDataConflicts + load conflicts from DB.
        List<Conflict> actualConflicts = calculateDataConflicts(refFromVersionId, refToVersionId, refToDraftId);
        assertConflicts(expectedConflicts, actualConflicts);
    }

    /*
     * testing calculate conflicts for two refBooks
     * published and versions of referenced refToRefBook have different PK
     *
     * exception is expected
     */
    //@Test // Отключён до нормальной реализации смены первичного ключа
    public void testCalculateConflictWhenPkChanged() {
        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");

        RefBook refToRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_to_diff_pk"));
        Integer refToVersionId = refToRefBook.getId();
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, id, null));
        updateData(refToVersionId, new Row(null, Map.of("ID", 1)), null);
        publish(refToVersionId, "1.0", LocalDateTime.now(), null, false);

        Structure.Attribute id_id = Structure.Attribute.buildPrimary("ID_ID", "id_id", FieldType.INTEGER, "id_id");
        Structure.Attribute ref_id = Structure.Attribute.build("REF_ID", "ref_id", FieldType.REFERENCE, "ref_id");
        Structure.Reference ref_id_ref = new Structure.Reference(ref_id.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(id.getCode()));

        RefBook refFromRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_from_diff_pk"));
        Integer refFromVersionId = refFromRefBook.getId();
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, id_id, null));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_id, ref_id_ref));
        updateData(refFromVersionId, new Row(null, Map.of("ID_ID", 1)), null);
        publish(refFromVersionId, "1.0", LocalDateTime.now(), null, false);

        Draft draft = draftService.create(
                new CreateDraftRequest(
                        refToRefBook.getRefBookId(),
                        new Structure(singletonList(id_id), emptyList()))
        );

        try {
            // NB: Use createCalculatedDataConflicts + load conflicts from DB.
            calculateDataConflicts(refFromVersionId, refToVersionId, draft.getId());
            fail();
        } catch (RestException re) {
            assertEquals("compare.primaries.not.match", re.getMessage());
        }
    }

    /*
     * testing check conflicts for two refBooks
     *
     * @see #testCalculateConflictWhenConflictsExist
     */
    @Test
    public void testCheckConflictWhenConflictsExist() {
        final String OLD_FILE_NAME = "oldRefToData.xlsx";
        final String NEW_FILE_NAME = "newRefToData.xlsx";
        final String REF_FILE_NAME = "refData.xlsx";

        Structure.Attribute id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        Structure.Attribute code = Structure.Attribute.build("CODE", "code", FieldType.STRING, "code");

        Structure structure = new Structure(asList(id, code), emptyList());

        RefBook refToRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_to_confl_chk"));
        Integer refToVersionId = refToRefBook.getId();
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, id, null));
        draftService.createAttribute(refToVersionId, new CreateAttributeRequest(null, code, null));

        updateFromFile(refToVersionId, null, OLD_FILE_NAME, "testConflicts/" + OLD_FILE_NAME);
        publish(refToVersionId, "1.0", LocalDateTime.now(), null, false);

        Integer refToDraftId = draftService.create(
                new CreateDraftRequest(
                        refToRefBook.getRefBookId(),
                        structure))
                .getId();
        updateFromFile(refToDraftId, null, NEW_FILE_NAME, "testConflicts/" + NEW_FILE_NAME);

        Structure.Attribute id_id = Structure.Attribute.buildPrimary("ID_ID", "id_id", FieldType.INTEGER, "id_id");
        Structure.Attribute ref_id_1 = Structure.Attribute.build("REF_ID_1", "ref_id_1", FieldType.REFERENCE, "ref_id_1");
        Structure.Attribute ref_id_2 = Structure.Attribute.build("REF_ID_2", "ref_id_2", FieldType.REFERENCE, "ref_id_2");
        Structure.Reference ref_id_1_ref = new Structure.Reference(ref_id_1.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(id.getCode()));
        Structure.Reference ref_id_2_ref = new Structure.Reference(ref_id_2.getCode(), refToRefBook.getCode(), DisplayExpression.toPlaceholder(id.getCode()));

        RefBook refFromRefBook = refBookService.create(createRefBookCreateRequest(CONFLICTS_REF_BOOK_CODE + "_from_confl_chk"));
        Integer refFromVersionId = refFromRefBook.getId();
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, id_id, null));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_id_1, ref_id_1_ref));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, ref_id_2, ref_id_2_ref));
        draftService.createAttribute(refFromVersionId, new CreateAttributeRequest(null, code, null));

        updateFromFile(refFromVersionId, null, REF_FILE_NAME, "testConflicts/" + REF_FILE_NAME);
        publish(refFromVersionId, "1.0", LocalDateTime.now(), null, false);

        Boolean actualUpdateCheck = conflictService.checkConflicts(refFromVersionId, refToVersionId,refToDraftId, ConflictType.UPDATED);
        assertEquals(Boolean.TRUE, actualUpdateCheck);

        Boolean actualDeleteCheck = conflictService.checkConflicts(refFromVersionId, refToVersionId, refToDraftId, ConflictType.DELETED);
        assertEquals(Boolean.TRUE, actualDeleteCheck);

        List<RefBookVersion> updatedReferrers = conflictService.getConflictingReferrers(refToDraftId, ConflictType.UPDATED);
        assertEquals(1, updatedReferrers.size());

        List<RefBookVersion> deletedReferrers = conflictService.getConflictingReferrers(refToDraftId, ConflictType.DELETED);
        assertEquals(1, deletedReferrers.size());
    }

    private List<Conflict> calculateDataConflicts(Integer refFromId, Integer oldRefToId, Integer newRefToId) {

        RefBookVersion refFromVersion = versionService.getById(refFromId);

        CalculateConflictCriteria criteria = new CalculateConflictCriteria(refFromId, oldRefToId, newRefToId);
        List<RefBookConflict> conflicts = conflictService.calculateDataConflicts(criteria);

        return conflicts.stream()
                .map(conflict -> {
                    RefBookRowValue rowValue = getSystemRowValue(refFromId, conflict.getRefRecordId());
                    if (Objects.isNull(rowValue))
                        return null;

                    List<FieldValue> primaryValues = FieldValueUtils.getRowPrimaryValues(rowValue, refFromVersion.getStructure());
                    return new Conflict(conflict.getRefFieldCode(), conflict.getConflictType(), primaryValues);

                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Получение записи по системному идентификатору.
     *
     * @param versionId идентификатор версии
     * @param systemId  системный идентификатор записи
     */
    private RefBookRowValue getSystemRowValue(Integer versionId, Long systemId) {
        if (versionId == null || systemId == null)
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setRowSystemIds(singletonList(systemId));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return (rowValues != null && !isEmpty(rowValues.getContent())) ? rowValues.getContent().get(0) : null;
    }

    /**
     * Создание справочника из файла XML.
     */
    @Test
    public void testCreateRefBookFromXmlFile() {

        String filename = "fromXml.xml";

        FileModel fileModel = createFileModel("testCreate_" + filename, "testCreate/" + filename);
        Draft expected = refBookService.create(fileModel);

        // Наличие черновика:
        Draft actual = draftService.getDraft(expected.getId());
        assertEquals(expected, actual);

        Page<RefBookRowValue> search = draftService.search(expected.getId(), new SearchDataCriteria());

        // Количество записей:
        assertEquals(2, search.getContent().size());

        // Совпадение записей:
        List<FieldValue> expectedData0 = new ArrayList<>() {{
            add(new StringFieldValue("TEST_CODE", "string2"));
            add(new IntegerFieldValue("number", 2));
            add(new DateFieldValue("dt", LocalDate.of(2002, 2, 2)));
            add(new BooleanFieldValue("flag", true));
        }};
        List actualData0 = search.getContent().get(0).getFieldValues();
        assertEquals(expectedData0, actualData0);

        List<FieldValue> expectedData1 = new ArrayList<>() {{
            add(new StringFieldValue("TEST_CODE", "string5"));
            add(new IntegerFieldValue("number", 5));
            add(new DateFieldValue("dt", LocalDate.of(2005, 5, 5)));
            add(new BooleanFieldValue("flag", false));
        }};
        List actualData1 = search.getContent().get(1).getFieldValues();
        assertEquals(expectedData1, actualData1);
    }

    /**
     * Создание справочника из файла XML неправильного формата.
     */
    @Test
    public void testCreateRefBookFromBadXmlFile() {

        // Общие проверки файла:
        failCreateRefBook("badNoExt", "file.extension.invalid");
        failCreateRefBook("badAnsi.xml", "file.content.invalid");

        // Проверки файла по блокам:
        failCreateRefBook("badRefBookCodeTag.xml", "file.content.invalid");
        failCreateRefBook("badPassportNameTag.xml", "file.content.invalid");
        failCreateRefBook("badStructureRowCodeTag.xml", "file.content.invalid");
        failCreateRefBook("badDataRowCodeTag.xml", "file.content.invalid");
    }

    private void failCreateRefBook(String filename, String message) {
        try {
            FileModel fileModel = createFileModel("testCreate_" + filename, "testCreate/" + filename);
            refBookService.create(fileModel);

            fail("Ожидается ошибка:\n" + message);

        } catch (RestException e) {

            assertEquals(message, getExceptionMessage(e));

        }
    }

    private boolean equalsFieldValues(List<Field> fields, List<FieldValue> values1, List<FieldValue> values2) {
        if (values1 == values2) return true;
        if (values1 == null || values2 == null || values1.size() != values2.size()) return false;

        for (FieldValue val1 : values1) {
            boolean isPresent = values2.stream().anyMatch(val2 -> {
                if (val2 == val1) return true;
                if (val2.getField().equals(val1.getField())) {
                    Field field = fields.stream().filter(f -> f.getName().equals(val2.getField())).findFirst().get();
                    //noinspection unchecked
                    return field.valueOf(val2.getValue()).equals(val1);
                }
                return false;

            });

            if (!isPresent) {
                return false;
            }
        }
        return true;
    }

    private void assertRefBooksEqual(RefBook expected, RefBook actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getRefBookId(), actual.getRefBookId());
        assertEquals(expected.getCode(), actual.getCode());
        assertPassportEqual(expected.getPassport(), actual.getPassport());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getComment(), actual.getComment());
        assertEquals(expected.getRemovable(), actual.getRemovable());
        assertEquals(expected.getArchived(), actual.getArchived());
        assertEquals(expected.getFromDate(), actual.getFromDate());
        assertEquals(expected.getLastPublishedVersionFromDate(), actual.getLastPublishedVersionFromDate());
    }

    private void assertPassportEqual(Map<String, String> expected, Map<String, String> actual) {
        if (expected == null) {
            assertNull(actual);

            return;
        }

        assertNotNull(actual);

        expected.forEach((k, v) -> {
            if (v == null)
                assertNull(actual.get(k));
            else
                assertNotNull(actual.get(k));
            assertEquals(v, actual.get(k));
        });
    }

    private void assertVersion(RefBookVersion expected, RefBookVersion actual) {
        assertEquals(expected.getRefBookId(), actual.getRefBookId());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getStatus(), actual.getStatus());
    }


    private void assertEqualRow(List<RowValue> expected, List<? extends RowValue> actual) {
        assertEquals(expected.size(), actual.size());
        Set<List> expectedStrings = expected.stream().map(RowValue::getFieldValues).collect(toSet());
        Set<List> actualStrings = actual.stream().map(RowValue::getFieldValues).collect(toSet());
        assertEquals(expectedStrings, actualStrings);
    }

    private void assertRows(List<Field> fields, List<RowValue> expectedRows, List<? extends RowValue> actualRows) {
        Assert.assertEquals("result size not equals", expectedRows.size(), actualRows.size());
        String expectedRowsStr = expectedRows.stream().map(RowValue::toString).collect(Collectors.joining(", "));
        String actualRowsStr = actualRows.stream().map(RowValue::toString).collect(Collectors.joining(", "));
        Assert.assertTrue(
                "not equals actualRows: \n" + actualRowsStr + " \n and expected rows: \n" + expectedRowsStr
                , actualRows.stream().anyMatch(actualRow ->
                        expectedRows.stream().anyMatch(expectedRow ->
                                equalsFieldValues(fields, expectedRow.getFieldValues(), actualRow.getFieldValues())
                        )
                )
        );
    }

    private void assertRefBookDataDiffs(RefBookDataDiff expectedRefBookDataDiff, RefBookDataDiff actualRefBookDataDiff) {

        assertListsEquals(expectedRefBookDataDiff.getNewAttributes(), actualRefBookDataDiff.getNewAttributes());
        assertListsEquals(expectedRefBookDataDiff.getOldAttributes(), actualRefBookDataDiff.getOldAttributes());
        assertListsEquals(expectedRefBookDataDiff.getUpdatedAttributes(), actualRefBookDataDiff.getUpdatedAttributes());

        assertDiffRowValues(expectedRefBookDataDiff.getRows().getContent(), actualRefBookDataDiff.getRows().getContent());
    }

    private void assertListsEquals(List<String> expectedValuesList, List<String> actualValuesList) {
        assertEquals(expectedValuesList.size(), actualValuesList.size());
        if (expectedValuesList.stream().anyMatch(expectedValue -> !actualValuesList.contains(expectedValue)))
            fail();
    }

    private void assertDiffRowValues(List<DiffRowValue> expectedDiffRowValues, List<DiffRowValue> actualDiffRowValues) {

        assertEquals(expectedDiffRowValues.size(), actualDiffRowValues.size());
        expectedDiffRowValues.forEach(expectedDiffRowValue -> {
            if (actualDiffRowValues.stream().noneMatch(actualDiffRowValue ->
                    expectedDiffRowValue.getValues().size() == actualDiffRowValue.getValues().size()
                            && actualDiffRowValue.getValues().containsAll(expectedDiffRowValue.getValues())))
                fail();
        });
    }

    /**
     * Проверка на совпадение списка конфликтов.
     * 
     * @param expectedList ожидаемый список
     * @param actualList   актуальный список
     */
    private void assertConflicts(List<Conflict> expectedList, List<Conflict> actualList) {
        assertNotNull(actualList);
        assertNotNull(expectedList);
        assertEquals(expectedList.size(), actualList.size());

        expectedList.forEach(expectedConflict -> {
            if (actualList.stream()
                    .noneMatch(actualConflict ->
                            Objects.equals(expectedConflict.getRefAttributeCode(), actualConflict.getRefAttributeCode())
                                    && Objects.equals(expectedConflict.getConflictType(), actualConflict.getConflictType())
                                    && expectedConflict.getPrimaryValues().size() == actualConflict.getPrimaryValues().size()
                                    && actualConflict.getPrimaryValues().containsAll(expectedConflict.getPrimaryValues())
                    ))
                fail();
        });
    }

    private void validateUpdateTypeWithoutException(Integer draftId, String attributeName,
                                                    Structure structure, FieldType newType,
                                                    Structure.Reference reference) {
        structure.getAttribute(attributeName).setType(newType);
        UpdateAttributeRequest request = new UpdateAttributeRequest(null, structure.getAttribute(attributeName), reference);
        draftService.updateAttribute(draftId, request);
    }

    private void validateUpdateTypeWithException(Integer draftId, String attributeName,
                                                 FieldType oldType, FieldType newType,
                                                 Structure.Reference reference) {
        Structure structure = versionService.getStructure(draftId);
        structure.getAttribute(attributeName).setType(newType);
        UpdateAttributeRequest request = new UpdateAttributeRequest(null, structure.getAttribute(attributeName), reference);
        try {
            draftService.updateAttribute(draftId, request);
            fail();

        } catch (Exception e) {
            logger.info("Тип " + getFieldTypeName(oldType) + " невозможно привести к типу " + getFieldTypeName(newType));
        }
    }

    private Structure createStructure() {
        Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, "description")));
        return structure;
    }

    private Structure createTestStructureWithSimpleTypesOnly() {
        return new Structure(
                new ArrayList<>(asList(
                        Structure.Attribute.build("string", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build("integer", "integer", FieldType.INTEGER, "число"),
                        Structure.Attribute.build("date", "date", FieldType.DATE, "дата"),
                        Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, "булево"),
                        Structure.Attribute.build("float", "float", FieldType.FLOAT, "дробное")
                )),
                null
        );
    }

    private void extendTestStructureForReferenceType(Structure structure) {

        if (!structure.hasPrimary() && structure.getAttribute("id") == null) {
            structure.add(Structure.Attribute.buildPrimary("id", "id", FieldType.INTEGER, "идентификатор"), null);
        }
        structure.add(
                Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, "ссылка"),
                new Structure.Reference("reference", TEST_REFERENCE_BOOK_CODE, toPlaceholder("count"))
        );
    }

    private void extendTestStructureForReferenceTypeAsString(Structure structure) {
        structure.getAttributes().add(
                Structure.Attribute.build("id", "id", FieldType.INTEGER, "идентификатор")
        );
        structure.getAttributes().add(
                Structure.Attribute.build("reference", "reference", FieldType.STRING, "строка-ссылка")
        );
    }

    private Structure createTestStructureWithReferenceType() {

        Structure structure = new Structure();
        extendTestStructureForReferenceType(structure);
        structure.add(Structure.Attribute.build("string", "string", FieldType.STRING, "строка"), null);

        return structure;
    }

    private Row createRowWithSimpleTypesOnly(String str, BigInteger bigInt,
                                             String date, Boolean bool, Double real) {
        return new Row(new HashMap<>() {{
            put("string", str);
            put("integer", bigInt);
            put("date", date != null ? parseLocalDate(date) : null);
            put("boolean", bool);
            put("float", real);
        }});
    }

    private void extendRowWithReferenceType(Row row, BigInteger id, Object ref) {

        Map<String, Object> map = row.getData();
        map.put("id", id);
        map.put("reference", ref);
    }

    private Row createRowWithReferenceType(BigInteger id, String str, Object ref) {

        Row row = new Row(new HashMap<>() {{
            put("string", str);
        }});
        extendRowWithReferenceType(row, id, ref);

        return row;
    }

    private List<RowValue> createOneStringFieldRow(String fieldName, String... values) {
        StringField stringField = new StringField(fieldName);

        List<RowValue> rows = new ArrayList<>();
        for (String s : values) {
            rows.add(new LongRowValue(stringField.valueOf(s)));
        }
        return rows;
    }

    private FileModel createFileModel(String path, String name) {
        try (InputStream input = ApplicationTest.class.getResourceAsStream("/" + name)) {
            return fileStorageService.save(input, path);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFieldTypeName(FieldType type) {
        switch (type) {
            case STRING:
                return "Строчный";
            case FLOAT:
                return "Дробный";
            case REFERENCE:
                return "Ссылочный";
            case INTEGER:
                return "Целочисленный";
            case BOOLEAN:
                return "Логический";
            case DATE:
                return "Дата";
            default:
                return null;
        }
    }

    private void publish(Integer draftId, String versionName,
                         LocalDateTime fromDate, LocalDateTime toDate,
                         boolean resolveConflicts) {
        PublishRequest request = new PublishRequest(null);
        request.setVersionName(versionName);
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setResolveConflicts(resolveConflicts);

        publishService.publish(draftId, request);
    }

    /**
     * Получение всех записей данных версии справочника.
     *
     * @param version версия
     * @return Список всех записей
     */
    private List<RefBookRowValue> getVersionAllRowContent(RefBookVersion version, Draft draft) {
        return getVersionAllRowContent(draft.getId(), draft.getStorageCode(), version.getStructure(),
                version.getFromDate(), version.getToDate());
    }

    private List<RefBookRowValue> getVersionAllRowContent(Integer versionId, String storageCode,
                                                          Structure structure,
                                                          LocalDateTime bdate, LocalDateTime edate) {
        StorageDataCriteria criteria = new StorageDataCriteria(storageCode, bdate, edate, fields(structure));
        criteria.setPage(DataCriteria.NO_PAGINATION_PAGE);
        criteria.setSize(DataCriteria.NO_PAGINATION_SIZE);

        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(criteria);
        if (pagedData.getCollection() == null)
            return null;

        return pagedData.getCollection().stream()
                .map(rowValue -> new RefBookRowValue((LongRowValue) rowValue, versionId))
                .collect(Collectors.toList());
    }

    /**
     * Получение записи данных по значению одного атрибута.
     *
     * @param versionId      идентификатор версии
     * @param attribute      атрибут (обычно первичный ключ)
     * @param attributeValue значение атрибута
     * @return Одна запись или null
     */
    private RefBookRowValue getVersionRowValue(Integer versionId, Structure.Attribute attribute, Serializable attributeValue) {

        if (versionId == null || attribute == null || attributeValue == null)
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();

        AttributeFilter filter = new AttributeFilter(attribute.getCode(), attributeValue, attribute.getType(), SearchTypeEnum.EXACT);
        criteria.addAttributeFilterList(singletonList(filter));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return (rowValues != null && !CollectionUtils.isEmpty(rowValues.getContent())) ? rowValues.getContent().get(0) : null;
    }

    private static String getRestExceptionMessage(RestException re) {

        if (!StringUtils.isEmpty(re.getMessage()))
            return re.getMessage();

        if (!isEmpty(re.getErrors()))
            return re.getErrors().get(0).getMessage();

        return null;
    }

    /** Получение кода сообщения об ошибке из исключения. */
    private static String getExceptionMessage(Exception e) {

        if (e instanceof UserException) {
            UserException ue = (UserException) e;

            if (!isEmpty(ue.getMessages()))
                return ue.getMessages().get(0).getCode();
        }

        if (!StringUtils.isEmpty(e.getMessage()))
            return e.getMessage();

        return null;
    }

    private RefBookCreateRequest createRefBookCreateRequest(String refBookCode) {
        return new RefBookCreateRequest(refBookCode, null, null);
    }

    private void updateData(Integer draftId, Row row, Integer optLockValue) {
        UpdateDataRequest request = new UpdateDataRequest(optLockValue, row);
        draftService.updateData(draftId, request);
    }

    private void updateData(Integer draftId, List<Row> rows, Integer optLockValue) {
        UpdateDataRequest request = new UpdateDataRequest(optLockValue, rows);
        draftService.updateData(draftId, request);
    }

    private void deleteData(Integer draftId, Row row, Integer optLockValue) {
        DeleteDataRequest request = new DeleteDataRequest(optLockValue, row);
        draftService.deleteData(draftId, request);
    }

    private void deleteAllData(Integer draftId, Integer optLockValue) {
        DeleteAllDataRequest request = new DeleteAllDataRequest(optLockValue);
        draftService.deleteAllData(draftId, request);
    }

    private void updateFromFile(Integer draftId, Integer optLockValue, String filePath, String fileName) {
        FileModel fileModel = createFileModel(filePath, fileName);
        UpdateFromFileRequest request = new UpdateFromFileRequest(optLockValue, fileModel);
        draftService.updateFromFile(draftId, request);
    }
}