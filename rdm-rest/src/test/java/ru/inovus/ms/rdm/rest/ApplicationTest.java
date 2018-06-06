package ru.inovus.ms.rdm.rest;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.inovus.ms.rdm.service.EchoService;

import java.io.IOException;

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
        EmbeddedPostgres.builder().setPort(5444).start();
    }

    @Test
    public void testIsRunning() throws Exception {
       Assert.assertEquals("SYSTEM RUNNING2",echoService.getEcho().getValue());
    }
}
