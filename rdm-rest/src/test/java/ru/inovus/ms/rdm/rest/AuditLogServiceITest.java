package ru.inovus.ms.rdm.rest;

import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.platform.test.autoconfigure.EnableEmbeddedPg;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.model.audit.AuditAction;
import ru.inovus.ms.rdm.model.audit.AuditLog;
import ru.inovus.ms.rdm.model.audit.AuditLogCriteria;
import ru.inovus.ms.rdm.service.api.AuditLogService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;

//@RunWith(SpringRunner.class)
//@SpringBootTest(
//        classes = Application.class,
//        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
//        properties = {
//                "cxf.jaxrs.client.classes-scan=true",
//                "cxf.jaxrs.client.classes-scan-packages=ru.inovus.ms.rdm.service.api",
//                "cxf.jaxrs.client.address=http://localhost:${server.port}/rdm/api",
//                "fileStorage.root=src/test/resources/rdm/temp",
//                "i18n.global.enabled=false"
//        })
//@DefinePort
//@EnableEmbeddedPg
//@Import(BackendConfiguration.class)
//@Transactional
public class AuditLogServiceITest {

    @Autowired
    @Qualifier("auditLogServiceJaxRsProxyClient")
    private AuditLogService auditLogService;

    private LocalDateTime date1 = LocalDateTime.of(2018, 3, 1, 0, 0);
    private LocalDateTime date2 = LocalDateTime.of(2018, 2, 1, 0, 0);
    private LocalDateTime date3 = LocalDateTime.of(2018, 1, 1, 0, 0);
    private LocalDateTime filterFromDate = LocalDateTime.of(2018, 2, 15, 0, 0);
    private AuditLog expected1 = new AuditLog(null, "user1", date1, AuditAction.PUBLICATION, "T001");
    private AuditLog expected2 = new AuditLog(null, "user2", date2, AuditAction.UPLOAD, "T003");
    private AuditLog expected3 = new AuditLog(null, "user3", date3, AuditAction.DOWNLOAD, "T002");

    /**
     * Тест добавления и чтения записи
     */
    @Test
    @Rollback
    public void testCreateReadAudit() {
        auditLogService.addAction(expected1);
        auditLogService.addAction(expected2);
        auditLogService.addAction(expected3);

        expected1.setId(1);
        expected2.setId(2);
        expected3.setId(3);

        List<AuditLog> expectedList = Arrays.asList(expected1, expected2, expected3);

        //сортировка по пользователям
        expectedList.sort(comparing(AuditLog::getUser));
        AuditLogCriteria criteria = new AuditLogCriteria();
        criteria.setOrders(singletonList(new Sort.Order(Sort.Direction.ASC, "user")));
        List<AuditLog> actualList = auditLogService.getActions(criteria).getContent();
        Assert.assertEquals(expectedList, actualList);

        //сортировка по времени
        expectedList.sort(comparing(AuditLog::getDate));
        criteria = new AuditLogCriteria();
        criteria.setOrders(singletonList(new Sort.Order(Sort.Direction.ASC, "date")));
        actualList = auditLogService.getActions(criteria).getContent();
        Assert.assertEquals(expectedList, actualList);

        //фильтр по пользователю
        AuditLogCriteria filterCriteria1 = new AuditLogCriteria();
        filterCriteria1.setUser("user2");
        Page<AuditLog> actualPage = auditLogService.getActions(filterCriteria1);
        Assert.assertEquals(1, actualPage.getTotalElements());
        Assert.assertEquals(expected2, actualPage.getContent().get(0));

        //фильтр по дате
        AuditLogCriteria filterCriteria2 = new AuditLogCriteria();
        filterCriteria2.setFromDate(filterFromDate);
        actualPage = auditLogService.getActions(filterCriteria2);
        Assert.assertEquals(1, actualPage.getTotalElements());
        Assert.assertEquals(expected1, actualPage.getContent().get(0));


    }
}