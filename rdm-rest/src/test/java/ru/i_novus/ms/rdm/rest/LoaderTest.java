package ru.i_novus.ms.rdm.rest;

import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.platform.test.autoconfigure.EnableEmbeddedPg;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.rest.loader.RefBookDataServerLoaderRunner;

import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "cxf.jaxrs.client.classes-scan=true",
                "cxf.jaxrs.client.classes-scan-packages=ru.i_novus.ms.rdm.api.rest, ru.i_novus.ms.rdm.api.service",
                "cxf.jaxrs.client.address=http://localhost:${server.port}/rdm/api",
                "spring.main.allow-bean-definition-overriding=true",
                "fileStorage.root=src/test/resources/rdm/temp",
                "i18n.global.enabled=false",
                "rdm.audit.disabledActions=all"
        })
@DefinePort
@EnableEmbeddedPg
@Import({BackendConfiguration.class, AppConfig.class})
@SuppressWarnings("FieldCanBeLocal")
public class LoaderTest {

    private final String LOADED_SUBJECT = "test";
    private final String LOADED_TARGET = "refBookData";

    private final String LOADED_CODE = "LOADED_DATA_";
    private final String LOADED_FILE_NAME = "loadedData_";
    private final String LOADED_FILE_EXT = ".xml";
    private final String LOADED_FILE_FOLDER = "src/test/resources/" + "testLoader/";

    @Autowired
    @Qualifier("refBookServiceJaxRsProxyClient")
    private RefBookService refBookService;

    @Autowired
    @Qualifier("versionRestServiceJaxRsProxyClient")
    private VersionRestService versionService;

    @Autowired
    private RefBookDataServerLoaderRunner refBookDataServerLoaderRunner;

    /** Успешная загрузка справочника из корректного xml. */
    @Test
    public void testRunMultipartBody() {

        int loadedFileSuccessIndex = 1;

        MultipartBody body = createBody(loadedFileSuccessIndex);
        refBookDataServerLoaderRunner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        String code = String.format("%s%d", LOADED_CODE, loadedFileSuccessIndex);
        try {
            Integer id = refBookService.getId(code);
            assertNotNull(id);

            RefBookVersion version = versionService.getLastPublishedVersion(code);
            assertNotNull(version);
            assertNotNull(version.getId());

        } catch (Exception e) {
            fail();
        }
    }

    /** Ошибка загрузки справочника из ошибочной xml (невалидный код атрибута). */
    @Test
    public void testRunMultipartBodyFailed() {

        int loadedFileAttributeCodeFailureIndex = 3;

        MultipartBody body = createBody(loadedFileAttributeCodeFailureIndex);
        try {
            refBookDataServerLoaderRunner.run(LOADED_SUBJECT, LOADED_TARGET, body);
            fail();

        } catch (UserException e) {
            assertTrue(e.getCode().contains("attribute.code.is.invalid"));

        } catch (Exception e) {
            fail();
        }
    }

    /** Пропуск загрузки существующего справочника из xml. */
    @Test
    public void testRunMultipartBodySkipped() {

        int loadedFileCodeExistsFailureIndex = 4;

        MultipartBody body = createBody(loadedFileCodeExistsFailureIndex);
        try {
            refBookDataServerLoaderRunner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        } catch (Exception e) {
            fail();
        }
    }

    private MultipartBody createBody(int index) {

        Attachment attachment = getFileAttachment(index);
        assertNotNull(attachment);

        return new MultipartBody(List.of(attachment), MediaType.MULTIPART_FORM_DATA_TYPE, false);
    }

    private Attachment getFileAttachment(int index) {

        try {
            Resource resource = new FileSystemResource(LOADED_FILE_FOLDER + getFileName(index));
            DataSource dataSource = new InputStreamDataSource(
                    resource.getInputStream(), MediaType.APPLICATION_XML, resource.getFilename()
            );

            String fileName = resource.getFilename();
            assertNotNull(fileName);

            MultivaluedMap<String, String> headers = new MultivaluedHashMap<>(1);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, List.of("filename=" + fileName));

            return new Attachment("file", dataSource, headers);

        } catch (IOException e) {
            return null;
        }
    }

    private String getFileName(int index) {
        return String.format("%s%d%s", LOADED_FILE_NAME, index, LOADED_FILE_EXT);
    }
}
