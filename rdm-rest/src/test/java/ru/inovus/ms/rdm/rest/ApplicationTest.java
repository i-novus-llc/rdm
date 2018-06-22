package ru.inovus.ms.rdm.rest;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.RefBookCreateRequest;
import ru.inovus.ms.rdm.model.RefBookCriteria;
import ru.inovus.ms.rdm.service.EchoService;
import ru.inovus.ms.rdm.service.RefBookService;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by tnurdinov on 31.05.2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5444/rdm_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "cxf.jaxrs.client.classes-scan=true",
        "cxf.jaxrs.client.classes-scan-packages=ru.inovus.ms.rdm",
        "cxf.jaxrs.client.address=http://localhost:8080/rdm/api"
} )
public class ApplicationTest extends AbstractIntegrationTest{

    @Autowired
    private EchoService echoService;

    @Test
    public void testIsRunning() throws Exception {

       Assert.assertEquals("SYSTEM RUNNING", echoService.getEcho().getValue());
    }


    @Autowired
    @Qualifier("rest")
    private RefBookService refBookService;

    @Test
    public void testLifecycle() {

        RefBookCreateRequest refBookCreateRequest = new RefBookCreateRequest();
        refBookCreateRequest.setCode("awesome");
        RefBook refBook = refBookService.create(refBookCreateRequest);
        assertNotNull(refBook.getCode());

        Page<RefBook> search = refBookService.search(new RefBookCriteria());
        assertEquals(1, search.getTotalElements());

        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setCode("awesome");
        search = refBookService.search(refBookCriteria);
        assertEquals(1, search.getTotalElements());

        refBookCriteria = new RefBookCriteria();
        refBookCriteria.setCode("notawesome");
        search = refBookService.search(refBookCriteria);
        assertEquals(0, search.getTotalElements());


    }
}
