package ru.inovus.ms.rdm.rest;

import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.platform.test.autoconfigure.EnableEmbeddedPg;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.FileStorageService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.util.TimeUtils.parseLocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "cxf.jaxrs.client.classes-scan=true",
                "cxf.jaxrs.client.classes-scan-packages=ru.inovus.ms.rdm.service.api",
                "cxf.jaxrs.client.address=http://localhost:${server.port}/rdm/api"
        })
@DefinePort
@EnableEmbeddedPg
@Import(BackendConfiguration.class)
public class ApplicationTest {

    private static final int REMOVABLE_REF_BOOK_ID = 501;
    private static final String REMOVABLE_REF_BOOK_CODE = "A082";
    private static final String SEARCH_CODE_STR = "78 ";
    private static final String SEARCH_BY_NAME_STR = "отличное от последней версии ";
    private static final String SEARCH_BY_NAME_STR_ASSERT_CODE = "Z001";
    private static final String PASSPORT_ATTRIBUTE_FULL_NAME = "fullName";
    private static final String PASSPORT_ATTRIBUTE_SHORT_NAME = "shortName";
    private static final String PASSPORT_ATTRIBUTE_ANNOTATION = "annotation";

    private static RefBookCreateRequest refBookCreateRequest;
    private static RefBookUpdateRequest refBookUpdateRequest;
    private static Structure.Attribute createAttribute;
    private static Structure.Reference createReference;

    private static Structure.Attribute updateAttribute;
    private static Structure.Attribute deleteAttribute;
    private static List<RefBookVersion> versionList;

    @Autowired
    @Qualifier("refBookServiceJaxRsProxyClient")
    private RefBookService refBookService;

    @Autowired
    @Qualifier("draftServiceJaxRsProxyClient")
    private DraftService draftService;

    @Autowired
    @Qualifier("versionServiceJaxRsProxyClient")
    private VersionService versionService;

    @Autowired
    @Qualifier("fileStorageServiceJaxRsProxyClient")
    private FileStorageService fileStorageService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private DraftDataService draftDataService;

    @BeforeClass
    public static void initialize() {
        refBookCreateRequest = new RefBookCreateRequest();
        refBookCreateRequest.setCode("T1");
        Map<String, String> createPassport = new HashMap<>();
        createPassport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "Справочник специальностей");
        createPassport.put(PASSPORT_ATTRIBUTE_ANNOTATION, "Аннотация для справочника специальностей");
        refBookCreateRequest.setPassport(new Passport(createPassport));

        refBookUpdateRequest = new RefBookUpdateRequest();
        refBookUpdateRequest.setCode(refBookCreateRequest.getCode() + "_upd");
        Map<String, String> updatePassport = new HashMap<>(createPassport);
        updatePassport.put(PASSPORT_ATTRIBUTE_SHORT_NAME, "СПРВЧНК СПЦЛНСТЙ  ");
        updatePassport.entrySet().forEach(e -> e.setValue(e.getValue() + "_upd"));
        updatePassport.put(PASSPORT_ATTRIBUTE_ANNOTATION, null);
        refBookUpdateRequest.setPassport(new Passport(updatePassport));
        refBookUpdateRequest.setComment("обновленное наполнение");

        createAttribute = Structure.Attribute.buildPrimary("name", "Наименование", FieldType.REFERENCE, "описание");
        createReference = new Structure.Reference(createAttribute.getCode(), 801, "code", emptyList(), emptyList());
        updateAttribute = Structure.Attribute.buildPrimary(createAttribute.getCode(),
                createAttribute.getName() + "_upd", createAttribute.getType(), createAttribute.getDescription() + "_upd");
        deleteAttribute = Structure.Attribute.build("code", "Код", FieldType.STRING, false, "на удаление");

        RefBookVersion version0 = new RefBookVersion();
        version0.setRefBookId(500);
        version0.setStatus(RefBookVersionStatus.DRAFT);
        version0.setDisplayStatus(RefBookVersionStatus.DRAFT.name());

        RefBookVersion version1 = new RefBookVersion();
        version1.setRefBookId(version0.getRefBookId());
        version1.setStatus(RefBookVersionStatus.PUBLISHED);
        version1.setVersion("3");

        RefBookVersion version2 = new RefBookVersion();
        version2.setRefBookId(version0.getRefBookId());
        version2.setStatus(RefBookVersionStatus.PUBLISHED);
        version2.setDisplayStatus(RefBookVersionStatus.PUBLISHED.name());
        version2.setVersion("2");

        RefBookVersion version3 = new RefBookVersion();
        version3.setRefBookId(version0.getRefBookId());
        version3.setStatus(RefBookVersionStatus.PUBLISHED);
        version3.setVersion("1");

        versionList = Arrays.asList(version0, version1, version2, version3);
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

        // создание справочника
        RefBook refBook = refBookService.create(refBookCreateRequest);
        assertNotNull(refBook.getId());
        assertNotNull(refBook.getRefBookId());
        assertEquals(refBookCreateRequest.getCode(), refBook.getCode());
        assertEquals(refBookCreateRequest.getPassport(), refBook.getPassport());
        assertEquals(RefBookVersionStatus.DRAFT, refBook.getStatus());
        assertEquals(RefBookStatus.DRAFT.getName(), refBook.getDisplayVersion());
        assertNull(refBook.getVersion());
        assertNull(refBook.getComment());
        assertTrue(refBook.getRemovable());
        assertFalse(refBook.getArchived());
        assertNull(refBook.getFromDate());
        Draft draft = draftService.getDraft(refBook.getId());
        assertNotNull(draft);
        assertNotNull(draft.getStorageCode());

        // изменение метеданных справочника
        refBookUpdateRequest.setId(refBook.getId());
        RefBook updatedRefBook = refBookService.update(refBookUpdateRequest);
        refBook.setCode(refBookUpdateRequest.getCode());
        Map<String, String> expectedAttributesAfterUpdate = new HashMap<>();
        expectedAttributesAfterUpdate.putAll(refBookCreateRequest.getPassport().getAttributes());
        expectedAttributesAfterUpdate.putAll(refBookUpdateRequest.getPassport().getAttributes());
        expectedAttributesAfterUpdate.entrySet().removeIf(e -> e.getValue() == null);
        refBook.setPassport(new Passport(expectedAttributesAfterUpdate));
        refBook.setComment(refBookUpdateRequest.getComment());
        refBook.setComment(refBookUpdateRequest.getComment());
        assertRefBooksEqual(refBook, updatedRefBook);

        // добавление атрибута
        CreateAttribute createAttributeModel = new CreateAttribute(draft.getId(), createAttribute, createReference);
        draftService.createAttribute(createAttributeModel);

        // получение структуры
        Structure structure = versionService.getStructure(draft.getId());

        // проверка добавленного атрибута
        assertEquals(1, structure.getAttributes().size());
        assertEquals(createAttribute, structure.getAttribute(createAttribute.getCode()));
        assertEquals(createReference, structure.getReference(createAttribute.getCode()));

        // изменение атрибута и проверка
        UpdateAttribute updateAttributeModel = new UpdateAttribute(draft.getId(), updateAttribute, createReference);
        draftService.updateAttribute(updateAttributeModel);
        structure = versionService.getStructure(draft.getId());
        assertEquals(updateAttribute, structure.getAttribute(updateAttributeModel.getCode()));
        assertEquals(createReference, structure.getReference(updateAttributeModel.getCode()));

        // удаление атрибута и проверка
        createAttributeModel.setAttribute(deleteAttribute);
        createAttributeModel.setReference(new Structure.Reference(null, null, null, null, null));
        draftService.createAttribute(createAttributeModel);

        draftService.deleteAttribute(draft.getId(), deleteAttribute.getCode());
        structure = versionService.getStructure(draft.getId());
        assertEquals(1, structure.getAttributes().size());

        // в архив
        refBookService.archive(refBook.getRefBookId());

        // получение по идентификатору версии
        RefBook refBookById = refBookService.getById(refBook.getId());
        refBook.setArchived(Boolean.TRUE);
        refBook.setRemovable(Boolean.FALSE);
        refBook.setDisplayVersion(RefBookStatus.ARCHIVED.getName());
        assertRefBooksEqual(refBook, refBookById);

        // удаление
        refBookService.delete(REMOVABLE_REF_BOOK_ID);
        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setCode(REMOVABLE_REF_BOOK_CODE);
        assertEquals(0, refBookService.search(criteria).getTotalElements());
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
        refBookCriteria.setRefBookId(500);
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
        nameCriteria.setPassport(new Passport(passportMap));
        Passport passport = new Passport(passportMap);
        RefBook refBook = refBookService.create(
                new RefBookCreateRequest(SEARCH_BY_NAME_STR_ASSERT_CODE, passport));

        search = refBookService.search(nameCriteria);
        assertEquals(1, search.getTotalElements());
        assertEquals(refBook.getPassport(), search.getContent().get(0).getPassport());

        // поиск по статусу 'Черновик'
        RefBookCriteria statusCriteria = new RefBookCriteria();
        statusCriteria.setStatus(RefBookStatus.DRAFT);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertTrue(RefBookVersionStatus.DRAFT.equals(r.getStatus())
                    || RefBookVersionStatus.PUBLISHING.equals(r.getStatus()));
            assertEquals(RefBookStatus.DRAFT.getName(), r.getDisplayVersion());
        });

        // поиск по статусу 'Архив'
        statusCriteria.setStatus(RefBookStatus.ARCHIVED);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertTrue(r.getArchived());
            assertFalse(r.getRemovable());
            assertEquals(RefBookStatus.ARCHIVED.getName(), r.getDisplayVersion());
        });

        // поиск по статусу 'Опубликован'
        statusCriteria.setStatus(RefBookStatus.PUBLISHED);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertNotNull(r.getLastPublishedVersionFromDate());
            assertFalse(r.getRemovable());
            assertNotNull(r.getDisplayVersion());
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

        // поиск по дате последней публикации (дата начала и дата окончания вне диапазона действия сущесвующих записей)
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
        Page<RefBookVersion> search = refBookService.getVersions(criteria);

        assertEquals(versionList.size(), search.getTotalElements());
        for (int i = 0; i < versionList.size(); i++) {
            RefBookVersion actual = search.getContent().get(i);
            assertVersion(versionList.get(i), actual);
        }

        // поиск с исключанием справочников
        criteria.setExcludeDraft(Boolean.TRUE);
        search = refBookService.getVersions(criteria);
        assertEquals(versionList.size() - 1, search.getTotalElements());

        // поиск по номеру версии
        criteria.setVersion(versionList.get(1).getVersion());
        search = refBookService.getVersions(criteria);
        assertEquals(1, search.getTotalElements());
    }

    private void assertRefBooksEqual(RefBook expected, RefBook actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getRefBookId(), actual.getRefBookId());
        assertEquals(expected.getCode(), actual.getCode());
        assertEquals(expected.getPassport(), actual.getPassport());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getDisplayVersion(), actual.getDisplayVersion());
        assertEquals(expected.getComment(), actual.getComment());
        assertEquals(expected.getRemovable(), actual.getRemovable());
        assertEquals(expected.getArchived(), actual.getArchived());
        assertEquals(expected.getFromDate(), actual.getFromDate());
        assertEquals(expected.getLastPublishedVersionFromDate(), actual.getLastPublishedVersionFromDate());
    }

    private void assertVersion(RefBookVersion expected, RefBookVersion actual) {
        assertEquals(expected.getRefBookId(), actual.getRefBookId());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getDisplayStatus(), actual.getDisplayStatus());
    }

    @Test
    public void testDraftCreate() {
        Structure structure = createStructure();
        Draft expected = draftService.create(1, structure);

        Draft actual = draftService.getDraft(expected.getId());

        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    public void testDraftRemove() {
        Structure structure = createStructure();
        Draft draft = draftService.create(1, structure);
        draftService.remove(draft.getId());
        assertNull(draftService.getDraft(draft.getId()));
    }

    private Structure createStructure() {
        Structure structure = new Structure();
        structure.setAttributes(Collections.singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, true, "description")));
        return structure;
    }

    @Test
    public void testVersionSearch() {
        Page<RowValue> rowValues = versionService.search(-1, new SearchDataCriteria());
        List<FieldValue> fieldValues = rowValues.getContent().get(0).getFieldValues();
        StringFieldValue name = new StringFieldValue("name", "name");
        IntegerFieldValue count = new IntegerFieldValue("count", 2);
        assertEquals(name.getValue(), fieldValues.get(0).getValue());
        assertEquals(count.getValue(), fieldValues.get(1).getValue());
    }

    @Test
    public void testPublishFirstDraft() throws Exception {
        new StringFieldValue();
        draftService.publish(-2, "1.0", LocalDateTime.now(), null);
        Page<RowValue> rowValuesInVersion = versionService.search(-1, OffsetDateTime.now(), new SearchDataCriteria());
        List fieldValues = rowValuesInVersion.getContent().get(0).getFieldValues();
        FieldValue name = new StringFieldValue("name", "name");
        FieldValue count = new IntegerFieldValue("count", 2);
        assertEquals(fieldValues.get(0), name);
        assertEquals(fieldValues.get(1), count);
        Page<RowValue> rowValuesOutVersion = versionService.search(-1, OffsetDateTime.now().minusDays(1), new SearchDataCriteria());
        assertEquals(new PageImpl<RowValue>(emptyList()), rowValuesOutVersion);
    }

    /**
     * Создаем новый черновик с ссылкой на опубликованную версию
     * Обновляем его данные из файла
     */
    @Test
    public void testDraftUpdateData() {
        int referenceVersion = -1;
        Structure structure = createStructure();
        structure.setAttributes(Arrays.asList(
                Structure.Attribute.build("string", "string", FieldType.STRING, false, "string"),
                Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, false, "count"),
                Structure.Attribute.build("float", "float", FieldType.FLOAT, false, "float"),
                Structure.Attribute.build("date", "date", FieldType.DATE, false, "date"),
                Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, false, "boolean")
        ));
        structure.setReferences(Collections.singletonList(new Structure.Reference("reference", referenceVersion, "count", Collections.singletonList("count"), null)));
        Draft draft = draftService.create(1, structure);
        FileModel fileModel = createFileModel("update_testUpload.xlsx", "testUpload.xlsx");

        draftService.updateData(draft.getId(), fileModel);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date = LocalDate.parse("01.01.2011", formatter);
        List<String> codes = structure.getAttributes().stream().map(Structure.Attribute::getCode).collect(Collectors.toList());
        Map<String, Object> rowMap1 = new HashMap<>();
        rowMap1.put(codes.get(0), "Иван");
        rowMap1.put(codes.get(1), new Reference("2", "2"));
        rowMap1.put(codes.get(2), 1.0);
        rowMap1.put(codes.get(3), date);
        rowMap1.put(codes.get(4), true);
        List<RowValue> expected = Collections.singletonList(ConverterUtil.rowValue(new Row(rowMap1), structure));

        Page<RowValue> search = draftService.search(draft.getId(), new SearchDataCriteria(null, null));
        assertRows(ConverterUtil.fields(structure), expected, search.getContent());
    }

    /**
     * Создаем новый черновик с ссылкой на опубликованную версию
     * Обновляем его данные из файла, который содержит невалидную ссылку
     */
    @Test()
    public void testDraftUpdateDataWithInvalidReference() {
        int referenceVersion = -1;
        Structure structure = createStructure();
        structure.setAttributes(Arrays.asList(
                Structure.Attribute.build("string", "string", FieldType.STRING, false, "string"),
                Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, false, "count"),
                Structure.Attribute.build("float", "float", FieldType.FLOAT, false, "float"),
                Structure.Attribute.build("date", "date", FieldType.DATE, false, "date"),
                Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, false, "boolean")
        ));
        structure.setReferences(Collections.singletonList(new Structure.Reference("reference", referenceVersion, "count", Collections.singletonList("count"), null)));
        Draft draft = draftService.create(1, structure);
        FileModel fileModel = createFileModel("update_testUploadInvalidReference.xlsx", "testUploadInvalidReference.xlsx");

        try {
            draftService.updateData(draft.getId(), fileModel);
        } catch (Exception e) {
            assertEquals("invalid.reference.err", e.getMessage());
        }

    }

    @Test
    public void testDraftCreateFromFile() {
        List<FieldValue> expectedData = new ArrayList() {{
            add(new StringFieldValue("string", "Иван"));
            add(new StringFieldValue("reference", "2"));
            add(new StringFieldValue("float", "1.0"));
            add(new StringFieldValue("date", "01.01.2011"));
            add(new StringFieldValue("boolean", "true"));
        }};
        FileModel fileModel = createFileModel("create_testUpload.xlsx", "testUpload.xlsx");
        Draft expected = draftService.create(-3, fileModel);
        Draft actual = draftService.getDraft(expected.getId());

        assertEquals(expected, actual);

        Page<RowValue> search = draftService.search(expected.getId(), new SearchDataCriteria());
        List actualData = search.getContent().get(0).getFieldValues();

        assertEquals(expectedData, actualData);

    }

    private FileModel createFileModel(String path, String name) {
        fileStorage.setRoot("src/test/resources/rdm");
        try (InputStream input = ApplicationTest.class.getResourceAsStream("/" + name)) {
            FileModel fileModel = new FileModel(path, name);
            String fullPath = fileStorageService.save(input, path);
            fileModel.setPath(fullPath);
            return fileModel;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void testSearchInDraft() {
        RefBookCreateRequest createRequest = new RefBookCreateRequest();
        createRequest.setCode("myTestCodeRefBook");
        Map<String, String> createPassport = new HashMap<>();
        createPassport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "Справочник для тестирования версий");
        createRequest.setPassport(new Passport(createPassport));

        RefBook refBook = refBookService.create(createRequest);
        Structure structure = getTestStructureWithoutTreeFieldType();
        Draft draft = draftService.create(refBook.getRefBookId(), structure);

        LocalDate localDateTime1 = LocalDate.of(2014, 9, 1);
        LocalDate localDateTime2 = LocalDate.of(2014, 10, 1);

        List<String> codes = structure.getAttributes().stream().map(Structure.Attribute::getCode).collect(Collectors.toList());
        Map<String, Object> rowMap1 = new HashMap<>();
        rowMap1.put(codes.get(0), 1);
        rowMap1.put(codes.get(1), 2.4);
        rowMap1.put(codes.get(2), "Первое тестовое наименование");
        rowMap1.put(codes.get(3), true);
        rowMap1.put(codes.get(4), localDateTime1);
        rowMap1.put(codes.get(5), new Reference("5", null));

        Map<String, Object> rowMap2 = new HashMap<>();
        rowMap2.put(codes.get(0), 2);
        rowMap2.put(codes.get(1), 0.4);
        rowMap2.put(codes.get(2), "Второе тестовое наименование");
        rowMap2.put(codes.get(3), false);
        rowMap2.put(codes.get(4), localDateTime2);
        rowMap2.put(codes.get(5), null);

        List<RowValue> rowValues = Arrays.asList(
                ConverterUtil.rowValue(new Row(rowMap1), structure),
                ConverterUtil.rowValue(new Row(rowMap2), structure));
        draftDataService.addRows(draft.getStorageCode(), rowValues);

        List<RowValue> expectedRowValues = Collections.singletonList(rowValues.get(0));

        List<Field> fields = ConverterUtil.fields(structure);

        codes.forEach(attributeCode -> {
            String fullTextSearchValue = FieldType.REFERENCE.equals(structure.getAttribute(attributeCode).getType()) ?
                    ((Reference) rowMap1.get(attributeCode)).getValue() : rowMap1.get(attributeCode).toString();
            Page<RowValue> actualPage = draftService.search(draft.getId(), new SearchDataCriteria(null, fullTextSearchValue));
            Assert.assertEquals("Full text search failed", 1, actualPage.getContent().size());
            assertRows(fields, expectedRowValues, actualPage.getContent());
        });

        List<AttributeFilter> attributeFilters = new ArrayList<>();
        codes.forEach(attributeCode -> {
            Object searchValue = FieldType.REFERENCE.equals(structure.getAttribute(attributeCode).getType()) ?
                    ((Reference) rowMap1.get(attributeCode)).getValue() : rowMap1.get(attributeCode);
            attributeFilters.add(new AttributeFilter(attributeCode, searchValue, structure.getAttribute(attributeCode).getType()));
        });

        attributeFilters.forEach(attributeFilter -> {
            Page<RowValue> actualPage = draftService.search(draft.getId(), new SearchDataCriteria(Collections.singletonList(attributeFilter), null));
            assertRows(fields, expectedRowValues, actualPage.getContent());
        });

        Page<RowValue> actualPage = draftService.search(draft.getId(), new SearchDataCriteria(attributeFilters, null));
        assertRows(fields, expectedRowValues, actualPage.getContent());
    }

    private Structure getTestStructureWithoutTreeFieldType() {
        return new Structure(
                Arrays.asList(
                        Structure.Attribute.buildPrimary("id", "idAttrName", FieldType.INTEGER, "idAttrDesc"),
                        Structure.Attribute.build("floatAttribute", "floatAttributeName", FieldType.FLOAT, false, "floatAttributeDesc"),
                        Structure.Attribute.build("name", "nameAttrName", FieldType.STRING, true, "nameAttrDesc"),
                        Structure.Attribute.build("booleanAttribute", "booleanAttributeName", FieldType.BOOLEAN, false, "booleanAttributeDesc"),
                        Structure.Attribute.build("dateAttribute", "dateAttributeName", FieldType.DATE, false, "dateAttributeDesc"),
                        Structure.Attribute.build("referenceAttribute", "referenceAttributeName", FieldType.REFERENCE, false, "referenceAttributeNameDesc")
                ),
                Collections.singletonList(createReference("referenceAttribute"))
        );
    }

    private Structure.Reference createReference(String attributeCode) {
        RefBookCreateRequest createRequest = new RefBookCreateRequest();
        createRequest.setCode("myTestCodeRefBookForRef");


        RefBook refBook = refBookService.create(createRequest);
        Structure structure = new Structure(
                Arrays.asList(
                        Structure.Attribute.buildPrimary("id", "idAttrName", FieldType.INTEGER, null),
                        Structure.Attribute.build("name", "nameAttrName", FieldType.STRING, false, null)
                ), null
        );
        Draft draft = draftService.create(refBook.getRefBookId(), structure);

        Map<String, Object> rowMap1 = new HashMap<>();
        rowMap1.put(structure.getAttributes().get(0).getCode(), 5);
        rowMap1.put(structure.getAttributes().get(1).getCode(), "запись для ссылки");

        draftDataService.addRows(draft.getStorageCode(),
                Collections.singletonList(ConverterUtil.rowValue(new Row(rowMap1), structure)));

        draftService.publish(draft.getId(), "1", LocalDateTime.of(2017, 9, 1, 0, 0), null);

        return new Structure.Reference(attributeCode, draft.getId(), structure.getAttributes().get(0).getCode(),
                Arrays.asList(structure.getAttributes().get(0).getCode(), structure.getAttributes().get(1).getCode()), null);
    }

    private void assertRows(List<Field> fields, List<RowValue> expectedRows, List<RowValue> actualRows) {
        Assert.assertEquals("result size not equals", expectedRows.size(), actualRows.size());
        String expectedRowsStr = expectedRows.stream().map(RowValue::toString).collect(Collectors.joining(", "));
        String actualRowsStr = actualRows.stream().map(RowValue::toString).collect(Collectors.joining(", "));
        Assert.assertTrue(
                "not equals actualRows: \n" + actualRowsStr + " \n and expected rows: \n" + expectedRowsStr
                , actualRows.stream().filter(actualRow ->
                                expectedRows.stream().filter(expectedRow ->
                                                equalsFieldValues(fields, expectedRow.getFieldValues(), actualRow.getFieldValues())
                                ).findAny().isPresent()
                ).findAny().isPresent()
        );
    }

    private boolean equalsFieldValues(List<Field> fields, List<FieldValue> values1, List<FieldValue> values2) {
        if (values1 == values2) return true;
        if (values1 == null || values2 == null || values1.size() != values2.size()) return false;

        for (FieldValue val1 : values1) {
            boolean isPresent = values2.stream().filter(val2 -> {
                if (val2 == val1) return true;
                if (val2.getField().equals(val1.getField())) {
                    Field field = fields.stream().filter(f -> f.getName().equals(val2.getField())).findFirst().get();
                    //noinspection unchecked
                    return field.valueOf(val2.getValue()).equals(val1);
                }
                return false;

            }).findAny().isPresent();

            if (!isPresent) {
                return false;
            }
        }
        return true;
    }
    @Test
    public void testCreateRequiredAttributeWithNotEmptyData(){
        CreateAttribute createAttributeModel = new CreateAttribute(-2, createAttribute, createReference);
        try {
            draftService.createAttribute(createAttributeModel);
        } catch (Exception e) {
            assertEquals("required.attribute.err", e.getMessage());
        }
    }

    @Test
    public void testValidatePrimaryKeyOnUpdateAttribute() {
        RefBookCreateRequest createRequest = new RefBookCreateRequest();
        createRequest.setCode("testValidatePrimaryKeyOnUpdateAttribute");
        Map<String, String> createPassport = new HashMap<>();
        createPassport.put(PASSPORT_ATTRIBUTE_FULL_NAME, "Справочник для тестирования проверки изменения первичного ключа");
        createRequest.setPassport(new Passport(createPassport));

        RefBook refBook = refBookService.create(createRequest);
        Structure structure = new Structure(
                Arrays.asList(
                        Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, null),
                        Structure.Attribute.build("name", "Наименование", FieldType.STRING, false, null),
                        Structure.Attribute.build("code", "Код", FieldType.STRING, false, null)),
                null);
        Draft draft = draftService.create(refBook.getRefBookId(), structure);

        List<String> codes = structure.getAttributes().stream().map(Structure.Attribute::getCode).collect(Collectors.toList());
        Map<String, Object> rowMap1 = new HashMap<>();
        rowMap1.put(codes.get(0), 1);
        rowMap1.put(codes.get(1), "Дублирующееся имя");
        rowMap1.put(codes.get(2), "001");

        Map<String, Object> rowMap2 = new HashMap<>();
        rowMap2.put(codes.get(0), 2);
        rowMap2.put(codes.get(1), "Дублирующееся имя");
        rowMap2.put(codes.get(2), "0021");

        List<RowValue> rowValues = Arrays.asList(
                ConverterUtil.rowValue(new Row(rowMap1), structure),
                ConverterUtil.rowValue(new Row(rowMap2), structure));

        draftDataService.addRows(draft.getStorageCode(), rowValues);

        structure.getAttribute("code").setPrimary(Boolean.TRUE);
        UpdateAttribute updateAttribute = new UpdateAttribute(draft.getId(), structure.getAttribute("name"), new Structure.Reference());
        draftService.updateAttribute(updateAttribute);

        structure.getAttribute("name").setPrimary(Boolean.TRUE);
        updateAttribute = new UpdateAttribute(draft.getId(), structure.getAttribute("name"), new Structure.Reference());
        try {
            draftService.updateAttribute(updateAttribute);
        } catch (Exception e) {
            assertEquals("primary.key.not.unique", e.getMessage());
        }
    }
}