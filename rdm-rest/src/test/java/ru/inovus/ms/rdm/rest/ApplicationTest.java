package ru.inovus.ms.rdm.rest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.RefBookService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.util.TimeUtils.parseLocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5444/rdm_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "cxf.jaxrs.client.classes-scan=true",
        "cxf.jaxrs.client.classes-scan-packages=ru.inovus.ms.rdm.service",
        "cxf.jaxrs.client.address=http://localhost:${server.port}/rdm/api",
        "server.port=8899"
})
public class ApplicationTest extends TestableDbEnv {

    private static final int REMOVABLE_REFBOOK_ID = 501;
    private static final String REMOVABLE_REF_BOOK_CODE = "A082";
    private static final String SEARCH_CODE_STR = "78 ";
    private static final String SEARCH_BY_NAME_STR = "отличное от последней версии ";
    private static final String SEARCH_BY_NAME_STR_ASSERT_CODE = "A080";

    private static RefBookUpdateRequest refBookRequest;
    private static List<RefBookVersion> versionList;

    @Autowired
    private RefBookService refBookService;

    @BeforeClass
    public static void initialize() {
        refBookRequest = new RefBookUpdateRequest();
        refBookRequest.setCode("T001");
        refBookRequest.setFullName("Справочник специальностей");
        refBookRequest.setShortName("СПРВЧНК СПЦЛНСТЙ");
        refBookRequest.setAnnotation("Аннотация для справочника специальностей");
        refBookRequest.setComment("обновленное наполнение");

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
     * Получение справоника по идентификатору версии.
     */

    @Test
    public void testLifecycle() {

        // создание справочника
        RefBook refBook = refBookService.create(refBookRequest);
        assertNotNull(refBook.getId());
        assertNotNull(refBook.getRefBookId());
        assertEquals(refBookRequest.getCode(), refBook.getCode());
        assertEquals(refBookRequest.getFullName(), refBook.getFullName());
        assertEquals(refBookRequest.getShortName(), refBook.getShortName());
        assertEquals(refBookRequest.getAnnotation(), refBook.getAnnotation());
        assertEquals(RefBookVersionStatus.DRAFT, refBook.getStatus());
        assertEquals(RefBookStatus.DRAFT.getName(), refBook.getDisplayVersion());
        assertNull(refBook.getVersion());
        assertNull(refBook.getComment());
        assertTrue(refBook.getRemovable());
        assertFalse(refBook.getArchived());
        assertNull(refBook.getFromDate());

        // изменение метеданных справочника
        refBookRequest.setId(refBook.getId());
        refBook.setComment(refBookRequest.getComment());
        RefBook updatedRefBook = refBookService.update(refBookRequest);
        assertRefBooksEqual(refBook, updatedRefBook);

        // в архив
        refBookService.archive(refBook.getRefBookId());

        // получение по идентификатору версии
        RefBook refBookById = refBookService.getById(refBook.getId());
        refBook.setArchived(Boolean.TRUE);
        refBook.setRemovable(Boolean.FALSE);
        refBook.setDisplayVersion(RefBookStatus.ARCHIVED.getName());
        assertRefBooksEqual(refBook, refBookById);

        // удаление
        refBookService.delete(REMOVABLE_REFBOOK_ID);
        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setCode(REMOVABLE_REF_BOOK_CODE);
        assertEquals(0, refBookService.search(criteria).getTotalElements());
    }

    /**
     * Поиск по наименованию.
     * Поиск по коду.
     * Поиск по статусу.
     * Поиск по дате последней публикации.
     */

    @Test
    public void testRefBookSearch() {

        // поиск по коду (по подстроке без учета регистра, крайние пробелы)
        RefBookCriteria codeCriteria = new RefBookCriteria();
        codeCriteria.setCode(SEARCH_CODE_STR);
        Page<RefBook> search = refBookService.search(codeCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> assertTrue(containsIgnoreCase(r.getCode(), codeCriteria.getCode().trim())));

        // поиск по наименованию
        RefBookCriteria nameCriteria = new RefBookCriteria();
        nameCriteria.setName(SEARCH_BY_NAME_STR.toUpperCase());
        search = refBookService.search(nameCriteria);
        assertEquals(1, search.getTotalElements());
        assertEquals(SEARCH_BY_NAME_STR_ASSERT_CODE, search.getContent().get(0).getCode());

        // поиск по статусу 'Черновик'
        RefBookCriteria statusCriteria = new RefBookCriteria();
        statusCriteria.setStatus(RefBookStatus.DRAFT);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertTrue(RefBookVersionStatus.DRAFT.equals(r.getStatus()) || RefBookVersionStatus.PUBLISHING.equals(r.getStatus()));
            assertEquals(RefBookStatus.DRAFT.getName(), r.getDisplayVersion());
        });

        // поиск по статусу 'Архив'
        statusCriteria.setStatus(RefBookStatus.ARCHIVED);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertTrue(r.getArchived());
            assertEquals(RefBookStatus.ARCHIVED.getName(), r.getDisplayVersion());
            assertFalse(r.getRemovable());
        });

        // поиск по статусу 'Опубликован'
        statusCriteria.setStatus(RefBookStatus.PUBLISHED);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertNotNull(r.getFromDate());
            assertNotNull(r.getDisplayVersion());
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
            assertTrue(r.getFromDate().equals(fromDateBegin) || r.getFromDate().isAfter(fromDateBegin));
            assertTrue(r.getFromDate().equals(fromDateEnd) || r.getFromDate().isBefore(fromDateEnd));
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
                assertTrue(r.getFromDate().equals(onlyFromDateBegin)
                        || r.getFromDate().isAfter(onlyFromDateBegin)));
    }

    /**
     * Получение списка версий справочника
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
    }

    private void assertVersion(RefBookVersion expected, RefBookVersion actual) {
        assertEquals(expected.getRefBookId(), actual.getRefBookId());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getDisplayStatus(), actual.getDisplayStatus());
    }
}
