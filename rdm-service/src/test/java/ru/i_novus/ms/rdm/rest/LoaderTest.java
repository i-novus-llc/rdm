package ru.i_novus.ms.rdm.rest;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.platform.test.autoconfigure.pg.EnableTestcontainersPg;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.rest.autoconfigure.config.AppConfig;
import ru.i_novus.ms.rdm.rest.autoconfigure.config.BackendConfiguration;
import ru.i_novus.ms.rdm.rest.loader.RefBookDataServerLoaderRunner;
import ru.i_novus.ms.rdm.service.Application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.util.loader.RefBookDataConstants.FIELD_REF_BOOK_CODE;
import static ru.i_novus.ms.rdm.api.util.loader.RefBookDataConstants.FIELD_REF_BOOK_FILE;

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
                "rdm.enable.publish.topic=false",
                "rdm.audit.disabledActions=all",
                "management.tracing.enabled=false"
        })
@DefinePort
@EnableTestcontainersPg
@ActiveProfiles("test")
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

    @Test
    public void testEmpty() {

        final int index = 1;

        final String code = getRefBookCode(index);
        assertNotNull(code);
    }

        /** Успешная загрузка справочника из корректного xml. */
    //@Test
    public void testRunMultipartBody() {

        final int loadedFileSuccessIndex = 1;

        final MultipartBody body = createBody(loadedFileSuccessIndex);
        refBookDataServerLoaderRunner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        final String code = getRefBookCode(loadedFileSuccessIndex);
        try {
            final Integer id = refBookService.getId(code);
            assertNotNull(id);

            final RefBookVersion version = versionService.getLastPublishedVersion(code);
            assertNotNull(version);
            assertNotNull(version.getId());

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /** Ошибка загрузки справочника из ошибочной xml (невалидный код атрибута). */
    //@Test
    public void testRunMultipartBodyFailed() {

        final int loadedFileAttributeCodeFailureIndex = 3;

        final MultipartBody body = createBody(loadedFileAttributeCodeFailureIndex);
        try {
            refBookDataServerLoaderRunner.run(LOADED_SUBJECT, LOADED_TARGET, body);
            fail();

        } catch (UserException e) {
            assertTrue(e.getCode().contains("attribute.code.is.invalid"));

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /** Пропуск загрузки существующего справочника из xml. */
    //@Test
    public void testRunMultipartBodySkipped() {

        final int loadedFileCodeExistsFailureIndex = 4;

        final MultipartBody body = createBody(loadedFileCodeExistsFailureIndex);
        try {
            refBookDataServerLoaderRunner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private MultipartBody createBody(int index) {

        final Attachment codeAttachment = getStringAttachment(FIELD_REF_BOOK_CODE, getRefBookCode(index));

        final Attachment fileAttachment = getFileAttachment(index);
        assertNotNull(fileAttachment);

        return new MultipartBody(
                List.of(codeAttachment, fileAttachment),
                MediaType.MULTIPART_FORM_DATA_TYPE,
                false
        );
    }

    private Attachment getStringAttachment(String name, String value) {

        final InputStream is = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        final DataSource dataSource = new InputStreamDataSource(is, MediaType.TEXT_PLAIN, name);

        return new StringAttachment(name, value, new DataHandler(dataSource));
    }

    private Attachment getFileAttachment(int index) {

        try {
            final Resource resource = new FileSystemResource(LOADED_FILE_FOLDER + getFileName(index));
            final DataSource dataSource = new InputStreamDataSource(
                    resource.getInputStream(), MediaType.APPLICATION_XML, resource.getFilename()
            );

            final String fileName = resource.getFilename();
            assertNotNull(fileName);

            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>(1);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, List.of("filename=" + fileName));

            return new Attachment(FIELD_REF_BOOK_FILE, dataSource, headers);

        } catch (IOException e) {
            return null;
        }
    }

    private String getRefBookCode(int index) {
        return String.format("%s%d", LOADED_CODE, index);
    }

    private String getFileName(int index) {
        return String.format("%s%d%s", LOADED_FILE_NAME, index, LOADED_FILE_EXT);
    }

    private static class StringAttachment extends Attachment {

        private final String value;

        public StringAttachment(String id, String value, DataHandler dataHandler) {

            super(id, dataHandler, new MultivaluedHashMap<>(0));
            this.value = value;
        }

        @Override
        public <T> T getObject(Class<T> cls) {
            return (T) value;
        }
    }
}
