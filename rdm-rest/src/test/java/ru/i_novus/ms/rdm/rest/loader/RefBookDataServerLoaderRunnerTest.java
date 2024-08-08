package ru.i_novus.ms.rdm.rest.loader;

import jakarta.activation.DataSource;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import net.n2oapp.platform.loader.server.ServerLoader;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.service.FileStorageService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.CREATE_ONLY;
import static ru.i_novus.ms.rdm.api.util.loader.RefBookDataConstants.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"rawtypes","java:S5778"})
public class RefBookDataServerLoaderRunnerTest extends BaseLoaderTest {

    private static final String LOADED_SUBJECT = "test";
    private static final String LOADED_TARGET = "refBookData";

    private static final int LOADED_FILE_SUCCESS_INDEX = 1;

    @InjectMocks
    private RefBookDataServerLoaderRunner runner;

    @Spy
    private final List<ServerLoader> loaders = createLoaders();

    @Mock
    private FileStorageService fileStorageService;

    private List<ServerLoader> createLoaders() {

        final List<ServerLoader> result = new ArrayList<>(1);

        final TestServerLoader loader = Mockito.spy(new TestServerLoader());
        result.add(loader);

        return result;
    }

    @Before
    public void setUp() {

        setField(runner, "loaderEnabled", true);
    }

    @Test
    public void testRunInputStream() {

        final InputStream body = new ByteArrayInputStream("body".getBytes());
        try {
            runner.run(LOADED_SUBJECT, LOADED_TARGET, body);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));

        } finally {
            try {
                body.close();

            } catch (IOException e) {
                fail();
            }
        }
    }

    @Test
    public void testRead() {

        final InputStream body = new ByteArrayInputStream("body".getBytes());
        try {
            runner.read(body, new TestServerLoader());
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));

        } finally {
            try {
                body.close();

            } catch (IOException e) {
                fail();
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunMultipartBodyWithJson() {

        final int index = LOADED_FILE_SUCCESS_INDEX;
        final RefBookDataRequest expected = createJsonDataRequest(index, CREATE_ONLY);

        final List<Attachment> attachments = createJsonAttachments(index);
        final MultipartBody body = new MultipartBody(attachments, MULTIPART_FORM_DATA_TYPE, false);

        runner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(loaders.get(0), times(1)).load(captor.capture(), eq(LOADED_SUBJECT));

        final List list = captor.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());

        final Object item = list.get(0);
        assertTrue(item instanceof RefBookDataRequest);

        final RefBookDataRequest actual = (RefBookDataRequest) item;
        assertObjects(Assert::assertEquals, expected, actual);
    }

    private List<Attachment> createJsonAttachments(int index) {

        return List.of(
                getPlainAttachment(index, FIELD_CHANGE_SET_ID, CHANGE_SET + index),
                getPlainAttachment(index, FIELD_UPDATE_TYPE, CREATE_ONLY.name().toLowerCase()),
                getPlainAttachment(index, FIELD_REF_BOOK_CODE, LOADED_CODE + index),
                getPlainAttachment(index, FIELD_REF_BOOK_NAME, LOADED_NAME + index),
                getPlainAttachment(index, FIELD_REF_BOOK_STRUCTURE, LOADED_STRUCTURE),
                getPlainAttachment(index, FIELD_REF_BOOK_DATA, LOADED_DATA)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunMultipartBodyWithFile() {

        final int index = LOADED_FILE_SUCCESS_INDEX;
        final RefBookDataRequest expected = createFileDataRequest(index, CREATE_ONLY);

        final FileModel fileModel = expected.getFileModel();
        when(fileStorageService.save(any(InputStream.class), eq(fileModel.getName()))).thenReturn(fileModel);

        final List<Attachment> attachments = createFileAttachments(index);
        final MultipartBody body = new MultipartBody(attachments, MULTIPART_FORM_DATA_TYPE, false);

        runner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(loaders.get(0), times(1)).load(captor.capture(), eq(LOADED_SUBJECT));

        final List list = captor.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());

        final Object item = list.get(0);
        assertTrue(item instanceof RefBookDataRequest);

        final RefBookDataRequest actual = (RefBookDataRequest) item;
        assertObjects(Assert::assertEquals, expected, actual);
    }

    private List<Attachment> createFileAttachments(int index) {

        final Attachment changeSetIdAttachment = getPlainAttachment(index, FIELD_CHANGE_SET_ID, CHANGE_SET + index);
        final Attachment updateTypeAttachment = getPlainAttachment(index, FIELD_UPDATE_TYPE, CREATE_ONLY.name().toLowerCase());
        final Attachment codeAttachment = getPlainAttachment(index, FIELD_REF_BOOK_CODE, LOADED_CODE + index);

        final Attachment fileAttachment = getFileAttachment(index);
        assertNotNull(fileAttachment);

        return List.of(changeSetIdAttachment, updateTypeAttachment, codeAttachment, fileAttachment);
    }

    private Attachment getPlainAttachment(int index, String name, String value) {

        final InputStream inputStream = new ByteArrayInputStream(value.getBytes());
        final DataSource dataSource = new InputStreamDataSource(inputStream, MediaType.TEXT_PLAIN, name);

        return new Attachment("attachment-id-" + index, dataSource, null);
    }

    private Attachment getFileAttachment(int index) {

        try {
            final Resource resource = new FileSystemResource(LOADED_FILE_FOLDER + getFileName(index));
            final DataSource dataSource = new InputStreamDataSource(
                    resource.getInputStream(), APPLICATION_XML, resource.getFilename()
            );

            final String fileName = resource.getFilename();
            assertNotNull(fileName);

            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>(1);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, List.of("filename=" + fileName));

            return new Attachment("file", dataSource, headers);

        } catch (IOException e) {
            return null;
        }
    }

    private static class TestServerLoader implements ServerLoader<RefBookDataRequest> {

        @Override
        public void load(List<RefBookDataRequest> data, String subject) {
            // Nothing to do.
        }

        @Override
        public String getTarget() {
            return LOADED_TARGET;
        }

        @Override
        public Class<RefBookDataRequest> getDataType() {
            return RefBookDataRequest.class;
        }
    }
}