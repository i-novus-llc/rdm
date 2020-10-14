package ru.i_novus.ms.rdm.rest.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.loader.server.ServerLoader;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.service.FileStorageService;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.test.BaseTest;

import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class RefBookDataServerLoaderRunnerTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LOADED_SUBJECT = "test";
    private static final String LOADED_TARGET = "refBookData";

    private static final String LOADED_CODE = "LOADED_DATA_";
    private static final String LOADED_NAME = "Loaded Name ";
    private static final String LOADED_STRUCTURE = "{}";
    private static final String LOADED_DATA = "{}";
    
    private static final String LOADED_FILE_NAME = "loadedData_";
    private static final String LOADED_FILE_EXT = ".xml";
    private static final String LOADED_FILE_FOLDER = "src/test/resources/" + "testLoader/";

    private static final int LOADED_FILE_SUCCESS_INDEX = 1;
    @Spy
    private final List<ServerLoader> loaders = createLoaders();
    @InjectMocks
    private RefBookDataServerLoaderRunner runner;
    @Mock
    private FileStorageService fileStorageService;

    private List<ServerLoader> createLoaders() {

        List<ServerLoader> result = new ArrayList<>(1);

        TestServerLoader loader = Mockito.spy(new TestServerLoader());
        result.add(loader);

        return result;
    }

    @Before
    public void setUp() throws NoSuchFieldException {

        JsonUtil.jsonMapper = objectMapper;

        FieldSetter.setField(runner, RefBookDataServerLoaderRunner.class.getDeclaredField("loaderEnabled"), true);
    }

    @Test
    public void testRunInputStream() {

        InputStream body = new ByteArrayInputStream("body".getBytes());
        try {
            runner.run(LOADED_SUBJECT, LOADED_TARGET, body);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }

    @Test
    public void testRead() {

        InputStream body = new ByteArrayInputStream("body".getBytes());
        try {
            runner.read(body, new TestServerLoader());
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunMultipartBodyWithJson() {

        int index = LOADED_FILE_SUCCESS_INDEX;
        RefBookDataRequest expected = createJsonDataRequest(index);

        List<Attachment> attachments = createJsonAttachments(index);
        MultipartBody body = new MultipartBody(attachments, MediaType.MULTIPART_FORM_DATA_TYPE, false);

        runner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(loaders.get(0), times(1)).load(captor.capture(), eq(LOADED_SUBJECT));

        List list = captor.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());

        Object item = list.get(0);
        assertTrue(item instanceof RefBookDataRequest);

        RefBookDataRequest actual = (RefBookDataRequest) item;
        assertObjects(Assert::assertEquals, expected, actual);
    }

    private RefBookDataRequest createJsonDataRequest(int index) {

        RefBookDataRequest expected = new RefBookDataRequest();

        expected.setCode(LOADED_CODE + index);
        expected.setPassport(new HashMap<>(1));
        expected.getPassport().put("name", LOADED_NAME + index);
        expected.setStructure(LOADED_STRUCTURE);
        expected.setData(LOADED_DATA);

        return expected;
    }

    private List<Attachment> createJsonAttachments(int index) {

        return List.of(
                getPlainAttachment(index, "code", LOADED_CODE + index),
                getPlainAttachment(index, "name", LOADED_NAME + index),
                getPlainAttachment(index, "structure", LOADED_STRUCTURE),
                getPlainAttachment(index, "data", LOADED_DATA)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunMultipartBodyWithFile() {

        int index = LOADED_FILE_SUCCESS_INDEX;

        RefBookDataRequest expected = createFileDataRequest(index);

        FileModel fileModel = expected.getFileModel();
        when(fileStorageService.save(any(InputStream.class), eq(fileModel.getName()))).thenReturn(fileModel);

        List<Attachment> attachments = createFileAttachments(index);
        MultipartBody body = new MultipartBody(attachments, MediaType.MULTIPART_FORM_DATA_TYPE, false);

        runner.run(LOADED_SUBJECT, LOADED_TARGET, body);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(loaders.get(0), times(1)).load(captor.capture(), eq(LOADED_SUBJECT));

        List list = captor.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());

        Object item = list.get(0);
        assertTrue(item instanceof RefBookDataRequest);

        RefBookDataRequest actual = (RefBookDataRequest) item;
        assertObjects(Assert::assertEquals, expected, actual);
    }

    private RefBookDataRequest createFileDataRequest(int index) {

        RefBookDataRequest expected = new RefBookDataRequest();

        expected.setCode(LOADED_CODE + index);
        expected.setPassport(emptyMap());

        String fileName = getFileName(index);
        FileModel fileModel = new FileModel(null, fileName);
        expected.setFileModel(fileModel);

        return expected;
    }

    private List<Attachment> createFileAttachments(int index) {

        Attachment codeAttachment = getPlainAttachment(index, "code", LOADED_CODE + index);
        Attachment fileAttachment = getFileAttachment(index);
        assertNotNull(fileAttachment);

        return List.of(codeAttachment, fileAttachment);
    }

    private Attachment getPlainAttachment(int index, String name, String value) {

        InputStream inputStream = new ByteArrayInputStream(value.getBytes());
        DataSource dataSource = new InputStreamDataSource(inputStream, MediaType.TEXT_PLAIN, name);

        return new Attachment("attachment-id-" + index, dataSource, null);
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