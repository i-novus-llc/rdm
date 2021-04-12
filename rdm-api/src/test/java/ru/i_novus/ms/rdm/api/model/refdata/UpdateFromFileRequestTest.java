package ru.i_novus.ms.rdm.api.model.refdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateFromFileRequestTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testClass() {

        UpdateFromFileRequest emptyRequest = new UpdateFromFileRequest();
        assertSpecialEquals(emptyRequest);

        UpdateFromFileRequest sameRequest = new UpdateFromFileRequest(emptyRequest.getOptLockValue(), emptyRequest.getFileModel());
        testEquals(emptyRequest, sameRequest);

        UpdateFromFileRequest newRequest = createRequest();
        assertNotEquals(emptyRequest, newRequest);

        sameRequest = createRequest();
        testEquals(newRequest, sameRequest);

        UpdateFromFileRequest copyRequest = copyRequest(newRequest);
        testEquals(newRequest, copyRequest);
    }

    private void testEquals(UpdateFromFileRequest expected, UpdateFromFileRequest actual) {

        if (expected == null) {
            assertNull(actual);

            return;
        }

        assertEquals(expected.toString(), actual.toString());

        assertEquals(expected.getOptLockValue(), actual.getOptLockValue());
        assertEquals(expected.getFileModel(), actual.getFileModel());
    }

    private UpdateFromFileRequest createRequest() {

        UpdateFromFileRequest result = new UpdateFromFileRequest();

        result.setOptLockValue(0);

        FileModel fileModel = new FileModel("path", "name");
        result.setFileModel(fileModel);

        return result;
    }

    private UpdateFromFileRequest copyRequest(UpdateFromFileRequest request) {

        UpdateFromFileRequest result = new UpdateFromFileRequest();

        result.setOptLockValue(request.getOptLockValue());
        result.setFileModel(request.getFileModel());

        return result;
    }
}