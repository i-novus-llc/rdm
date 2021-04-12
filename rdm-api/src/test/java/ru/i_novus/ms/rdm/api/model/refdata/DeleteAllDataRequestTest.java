package ru.i_novus.ms.rdm.api.model.refdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DeleteAllDataRequestTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testClass() {

        DeleteAllDataRequest emptyRequest = new DeleteAllDataRequest();
        assertSpecialEquals(emptyRequest);

        DeleteAllDataRequest sameRequest = new DeleteAllDataRequest(emptyRequest.getOptLockValue());
        testEquals(emptyRequest, sameRequest);

        DeleteAllDataRequest newRequest = createRequest();
        assertNotEquals(emptyRequest, newRequest);

        sameRequest = createRequest();
        testEquals(newRequest, sameRequest);

        DeleteAllDataRequest copyRequest = copyRequest(newRequest);
        testEquals(newRequest, copyRequest);
    }

    private void testEquals(DeleteAllDataRequest expected, DeleteAllDataRequest actual) {

        if (expected == null) {
            assertNull(actual);

            return;
        }

        assertEquals(expected.toString(), actual.toString());

        assertEquals(expected.getOptLockValue(), actual.getOptLockValue());
    }

    private DeleteAllDataRequest createRequest() {

        DeleteAllDataRequest result = new DeleteAllDataRequest();

        result.setOptLockValue(0);

        return result;
    }

    private DeleteAllDataRequest copyRequest(DeleteAllDataRequest request) {

        DeleteAllDataRequest result = new DeleteAllDataRequest();

        result.setOptLockValue(request.getOptLockValue());

        return result;
    }
}