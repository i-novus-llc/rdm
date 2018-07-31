package ru.inovus.ms.rdm.rest;

import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.platform.test.autoconfigure.EnableEmbeddedPg;
import org.apache.commons.io.IOUtils;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.*;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    private static final String SEARCH_BY_NAME_STR_ASSERT_CODE = "A080";
    private static final String SEARCH_BY_CODE_AND_NAME = "A080 Справочник МО";

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
    private FileStorage fileStorage;

    @BeforeClass
    public static void initialize() {
        refBookCreateRequest = new RefBookCreateRequest();
        refBookCreateRequest.setCode("T1");
        refBookCreateRequest.setFullName("Справочник специальностей");
        refBookCreateRequest.setShortName("СПРВЧНК СПЦЛНСТЙ  ");
        refBookCreateRequest.setAnnotation("Аннотация для справочника специальностей");

        refBookUpdateRequest = new RefBookUpdateRequest();
        refBookUpdateRequest.setCode(refBookCreateRequest.getCode() + "_upd");
        refBookUpdateRequest.setFullName(refBookCreateRequest.getFullName() + "_upd");
        refBookUpdateRequest.setShortName(refBookCreateRequest.getShortName() + "_upd");
        refBookUpdateRequest.setAnnotation(refBookCreateRequest.getAnnotation() + "_upd");
        refBookUpdateRequest.setComment("обновленное наполнение");

        createAttribute = Structure.Attribute.buildPrimary("name", "Наименование", FieldType.REFERENCE, "описание");
        createReference = new Structure.Reference(createAttribute.getCode(), 801, "code", null);
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
        assertEquals(refBookCreateRequest.getFullName(), refBook.getFullName());
        assertEquals(refBookCreateRequest.getShortName(), refBook.getShortName());
        assertEquals(refBookCreateRequest.getAnnotation(), refBook.getAnnotation());
        assertEquals(RefBookVersionStatus.DRAFT, refBook.getStatus());
        assertEquals(RefBookStatus.DRAFT.getName(), refBook.getDisplayVersion());
        assertNull(refBook.getVersion());
        assertNull(refBook.getComment());
        assertTrue(refBook.getRemovable());
        assertFalse(refBook.getArchived());
        assertNull(refBook.getFromDate());
        assertNotNull(draftService.getDraft(refBook.getId()).getStorageCode());

        // изменение метеданных справочника
        refBookUpdateRequest.setId(refBook.getId());
        RefBook updatedRefBook = refBookService.update(refBookUpdateRequest);
        refBook.setCode(refBookUpdateRequest.getCode());
        refBook.setFullName(refBookUpdateRequest.getFullName());
        refBook.setShortName(refBookUpdateRequest.getShortName());
        refBook.setAnnotation(refBookUpdateRequest.getAnnotation());
        refBook.setComment(refBookUpdateRequest.getComment());
        refBook.setComment(refBookUpdateRequest.getComment());
        assertRefBooksEqual(refBook, updatedRefBook);

        // добавление атрибута
        draftService.createAttribute(refBook.getId(), createAttribute,
                createReference.getReferenceVersion(), createReference.getReferenceAttribute(), null);

        // получение структуры
        Structure structure = versionService.getStructure(refBook.getId());

        // проверка добавленного атрибута
        assertEquals(1, structure.getAttributes().size());
        assertEquals(createAttribute, structure.getAttributes().get(0));
        createReference.setDisplayAttributes(Collections.singletonList(createReference.getReferenceAttribute()));
        assertEquals(createReference, structure.getReference(createAttribute.getCode()));

        // изменение атрибута и проверка
        draftService.updateAttribute(refBook.getId(), updateAttribute, createReference.getReferenceVersion(),
                createReference.getReferenceAttribute(), createReference.getDisplayAttributes());
        structure = versionService.getStructure(refBook.getId());
        assertEquals(updateAttribute, structure.getAttributes().get(0));
        assertEquals(createReference, structure.getReference(updateAttribute.getCode()));

        // удадение атрибута и проверка
        draftService.createAttribute(refBook.getId(), deleteAttribute, null, null, null);

        draftService.deleteAttribute(refBook.getId(), deleteAttribute.getCode());
        structure = versionService.getStructure(refBook.getId());
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

        // поиск по наименованию
        RefBookCriteria nameCriteria = new RefBookCriteria();
        nameCriteria.setName(SEARCH_BY_NAME_STR.toUpperCase());
        search = refBookService.search(nameCriteria);
        assertEquals(1, search.getTotalElements());
        assertEquals(SEARCH_BY_NAME_STR_ASSERT_CODE, search.getContent().get(0).getCode());

        // поиск по коду и наименованию
        RefBookCriteria codeNameCriteria = new RefBookCriteria();
        codeNameCriteria.setName(SEARCH_BY_CODE_AND_NAME.toUpperCase());
        search = refBookService.search(nameCriteria);
        assertEquals(1, search.getTotalElements());
        assertEquals(SEARCH_BY_CODE_AND_NAME, search.getContent().get(0).getCodeName());

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
        assertEquals(expected.getFullName(), actual.getFullName());
        assertEquals(expected.getShortName(), actual.getShortName());
        assertEquals(expected.getAnnotation(), actual.getAnnotation());
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
        assertEquals(new PageImpl<RowValue>(Collections.emptyList()), rowValuesOutVersion);
    }

    /**
     * Публикуем черновик, на который будем ссылаться
     * Создаем новый черновик с ссылкой
     * Обновляем его данные
     */
    @Test
    public void testDraftUpdateData() {
        int referenceVersion = -3;
        draftService.publish(referenceVersion, "1.0", LocalDateTime.now(), null);
        Structure structure = createStructure();
        structure.setAttributes(Arrays.asList(
            Structure.Attribute.build("string", "string", FieldType.STRING, false, "string"),
            Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, false, "count"),
            Structure.Attribute.build("float", "float", FieldType.FLOAT, false, "float"),
            Structure.Attribute.build("date", "date", FieldType.DATE, false, "date"),
            Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, false, "boolean")
        ));
        structure.setReferences(Collections.singletonList(new Structure.Reference("reference", referenceVersion, "count", Collections.singletonList("count"))));
        Draft draft = draftService.create(1, structure);
        FileModel fileModel = new FileModel("testUpload.xlsx", "testUpload.xlsx");
        try(InputStream input = ApplicationTest.class.getResourceAsStream("/testUpload.xlsx")){
            String fullPath = fileStorage.saveContent(input, fileModel.getPath());
            fileModel.setPath(fullPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        draftService.updateData(draft.getId(), fileModel);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date =  LocalDate.parse("01.01.2011", formatter);
        List<FieldValue> expected = new ArrayList(){{
            add(new StringFieldValue("string", "Иван"));
            add(new ReferenceFieldValue("reference", new Reference("2", "2")));
            add(new FloatFieldValue("float", new Double("1.0")));
            add(new DateFieldValue("date", date));
            add(new BooleanFieldValue("boolean", Boolean.TRUE));
        }};
        Page<RowValue> search = draftService.search(draft.getId(), new SearchDataCriteria(null, null));
        List actual = search.getContent().get(0).getFieldValues();
        Assert.assertEquals(expected, actual);
    }

    private MultipartFile createMultipartFile() {
        try (InputStream input = ApplicationTest.class.getResourceAsStream("/testUpload.xlsx")) {
            return new MockMultipartFile("testUpload",
                    "testUpload.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", IOUtils.toByteArray(input));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testDraftCreateFromFile(){
        List<FieldValue> expectedData = new ArrayList(){{
            add(new StringFieldValue("string", "Иван"));
            add(new StringFieldValue("reference", "2"));
            add(new StringFieldValue("float", "1.0"));
            add(new StringFieldValue("date", "01.01.2011"));
            add(new StringFieldValue("boolean", "true"));
        }};
        Draft expected = draftService.create(-3, createMultipartFile());
        Draft actual = draftService.getDraft(expected.getId());

        Assert.assertEquals(expected, actual);

        Page<RowValue> search = draftService.search(expected.getId(), new SearchDataCriteria());
        List actualData = search.getContent().get(0).getFieldValues();

        Assert.assertEquals(expectedData, actualData);

    }

}