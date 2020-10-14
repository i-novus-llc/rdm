package ru.i_novus.ms.rdm.api.model.refdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.test.BaseTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDataRequestTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testClass() {

        UpdateDataRequest emptyRequest = new UpdateDataRequest();
        assertSpecialEquals(emptyRequest);

        UpdateDataRequest sameRequest = new UpdateDataRequest(emptyRequest.getOptLockValue(), emptyRequest.getRows());
        testEquals(emptyRequest, sameRequest);

        UpdateDataRequest newRequest = createRequest();
        assertNotEquals(emptyRequest, newRequest);

        sameRequest = createRequest();
        testEquals(newRequest, sameRequest);

        UpdateDataRequest rowRequest = new UpdateDataRequest(newRequest.getOptLockValue(), newRequest.getRows().get(0));
        assertNotEquals(newRequest, rowRequest);

        UpdateDataRequest copyRequest = copyRequest(newRequest);
        testEquals(newRequest, copyRequest);
    }

    private void testEquals(UpdateDataRequest expected, UpdateDataRequest actual) {

        if (expected == null) {
            assertNull(actual);

            return;
        }

        assertEquals(expected.toString(), actual.toString());

        assertEquals(expected.getOptLockValue(), actual.getOptLockValue());
        assertEquals(expected.getRows(), actual.getRows());
    }

    private UpdateDataRequest createRequest() {

        UpdateDataRequest result = new UpdateDataRequest();

        result.setOptLockValue(0);

        List<Row> rows = IntStream.range(0, 2).mapToObj(this::createRow).collect(toList());
        result.setRows(rows);

        return result;
    }

    private Row createRow(int index) {

        Map<String, Object> data = new HashMap<>(1);
        data.put("id", index * 10);

        return new Row((long) index, data);
    }

    private UpdateDataRequest copyRequest(UpdateDataRequest request) {

        UpdateDataRequest result = new UpdateDataRequest();

        result.setOptLockValue(request.getOptLockValue());
        result.setRows(request.getRows());

        return result;
    }
}