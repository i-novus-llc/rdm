package ru.inovus.ms.rdm.rest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.RefBookCreateRequest;
import ru.inovus.ms.rdm.model.RefBookCriteria;
import ru.inovus.ms.rdm.model.RefBookVersionStatus;
import ru.inovus.ms.rdm.service.EchoService;
import ru.inovus.ms.rdm.service.RefBookService;

import java.time.LocalDateTime;

import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.util.TimeUtils.parseLocalDateTime;

/**
 * Created by tnurdinov on 31.05.2018.
 */
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
public class ApplicationTest extends AbstractIntegrationTest {

    private static RefBookCreateRequest refBookCreateRequest;

    @Autowired
    private EchoService echoService;

    @Autowired
    private RefBookService refBookService;

    @BeforeClass
    public static void initialize() {
        refBookCreateRequest = new RefBookCreateRequest();
        refBookCreateRequest.setCode("T001");
        refBookCreateRequest.setFullName("Справочник специальностей");
        refBookCreateRequest.setShortName("СПРВЧНК СПЦЛНСТЙ");
        refBookCreateRequest.setAnnotation("Аннотация для справочника специальностей");
    }

    @Test
    public void testIsRunning() throws Exception {

        Assert.assertEquals("SYSTEM RUNNING", echoService.getEcho().getValue());
    }

    /**
     * Создание справочника.
     * Получение справоника по идентификатору версии.
     */

    @Test
    public void testLifecycle() {

        // создание справочника
        RefBook refBook = refBookService.create(refBookCreateRequest);
        assertNotNull(refBook.getRefBookId());
        assertTrue(refBook.getRemovable());
        assertFalse(refBook.getArchived());
        assertEquals(refBookCreateRequest.getCode(), refBook.getCode());
        assertEquals(refBookCreateRequest.getFullName(), refBook.getFullName());
        assertEquals(refBookCreateRequest.getShortName(), refBook.getShortName());
        assertEquals(refBookCreateRequest.getAnnotation(), refBook.getAnnotation());
        assertEquals(RefBookVersionStatus.DRAFT, refBook.getStatus());
        assertEquals(RefBookStatus.DRAFT.getName(), refBook.getVersion());

        // получение по идентификатору версии
        RefBook refBookById = refBookService.getById(refBook.getId());
        assertEquals(refBook, refBookById);
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
        codeCriteria.setCode("78 ");
        Page<RefBook> search = refBookService.search(codeCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> assertTrue(containsIgnoreCase(r.getCode(), codeCriteria.getCode().trim())));

        // поиск по наименованию
        RefBookCriteria nameCriteria = new RefBookCriteria();
        nameCriteria.setName("отличное от последней версии ".toUpperCase());
        search = refBookService.search(nameCriteria);
        assertEquals(1, search.getTotalElements());
        assertEquals("A080", search.getContent().get(0).getCode());

        // поиск по статусу 'Черновик'
        RefBookCriteria statusCriteria = new RefBookCriteria();
        statusCriteria.setStatus(RefBookStatus.DRAFT);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertTrue(RefBookVersionStatus.DRAFT.equals(r.getStatus()) || RefBookVersionStatus.PUBLISHING.equals(r.getStatus()));
            assertEquals(RefBookStatus.DRAFT.getName(), r.getVersion());
        });

        // поиск по статусу 'Архив'
        statusCriteria.setStatus(RefBookStatus.ARCHIVED);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertTrue(r.getArchived());
            assertEquals(RefBookStatus.ARCHIVED.getName(), r.getVersion());
            assertFalse(r.getRemovable());
        });

        // поиск по статусу 'Опубликован'
        statusCriteria.setStatus(RefBookStatus.PUBLISHED);
        search = refBookService.search(statusCriteria);
        assertTrue(search.getTotalElements() > 0);
        search.getContent().forEach(r -> {
            assertFalse(r.getArchived());
            assertNotNull(r.getFromDate());
            assertNotNull(r.getVersion());
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
}
