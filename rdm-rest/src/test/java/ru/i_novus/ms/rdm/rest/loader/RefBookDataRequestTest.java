package ru.i_novus.ms.rdm.rest.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.test.BaseTest;

import java.util.HashMap;

public class RefBookDataRequestTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testRefBookDataRequest() {

        RefBookDataRequest emptyRequest = new RefBookDataRequest();
        assertSpecialEquals(emptyRequest);

        RefBookDataRequest newRequest = createRequest();
        assertObjects(Assert::assertNotEquals, emptyRequest, newRequest);
    }

    private RefBookDataRequest createRequest() {

        RefBookDataRequest request = new RefBookDataRequest();

        request.setCode("TEST");
        request.setPassport(new HashMap<>(1));
        request.getPassport().put("name", "Test");
        request.setStructure("{}");
        request.setData("{}");

        FileModel fileModel = new FileModel("filePath", "fileName");
        request.setFileModel(fileModel);

        return request;
    }
}