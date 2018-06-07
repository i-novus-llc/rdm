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
import ru.inovus.ms.rdm.model.ReferenceBook;
import ru.inovus.ms.rdm.model.ReferenceBookCreateRequest;
import ru.inovus.ms.rdm.model.ReferenceBookCriteria;
import ru.inovus.ms.rdm.service.EchoService;
import ru.inovus.ms.rdm.service.ReferenceBookService;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by tnurdinov on 31.05.2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5444/postgres",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "cxf.jaxrs.client.classes-scan=true",
        "cxf.jaxrs.client.classes-scan-packages=ru.inovus.ms.rdm",
        "cxf.jaxrs.client.address=http://localhost:8080/rdm/api"
} )
public class ApplicationTest {

    @Autowired
    @Qualifier("EchoRest")
    private EchoService echoService;

    @BeforeClass
    public static void startDb() throws IOException {
        EmbeddedPostgres.builder().setCleanDataDirectory(true).setPort(5444).start();
    }

    @Test
    public void testIsRunning() throws Exception {

       Assert.assertEquals("SYSTEM RUNNING",echoService.getEcho().getValue());
    }


    @Autowired
    @Qualifier("rest")
    private ReferenceBookService referenceBookService;

    @Test
    public void testLifecycle() {

        ReferenceBookCreateRequest referenceBookCreateRequest = new ReferenceBookCreateRequest();
        referenceBookCreateRequest.setCode("awesome");
        ReferenceBook referenceBook = referenceBookService.create(referenceBookCreateRequest);
        assertNotNull(referenceBook.getCode());

        Page<ReferenceBook> search = referenceBookService.search(new ReferenceBookCriteria());
        assertEquals(1, search.getTotalElements());

        ReferenceBookCriteria referenceBookCriteria = new ReferenceBookCriteria();
        referenceBookCriteria.setCode("awesome");
        search = referenceBookService.search(referenceBookCriteria);
        assertEquals(1, search.getTotalElements());

        referenceBookCriteria = new ReferenceBookCriteria();
        referenceBookCriteria.setCode("notawesome");
        search = referenceBookService.search(referenceBookCriteria);
        assertEquals(0, search.getTotalElements());


    }
}
