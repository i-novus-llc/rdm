package ru.i_novus.ms.rdm.api.model.loader;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;

import java.util.HashMap;

import static org.junit.Assert.assertNotEquals;

public class RefBookDataRequestTest extends BaseTest {

    @Test
    public void testClass() {

        RefBookDataRequest emptyRequest = new RefBookDataRequest();
        assertSpecialEquals(emptyRequest);

        RefBookDataRequest newRequest = createRequest();
        assertObjects(Assert::assertNotEquals, emptyRequest, newRequest);

        RefBookDataRequest copyRequest = copyRequest(newRequest);
        assertObjects(Assert::assertEquals, newRequest, copyRequest);
    }

    @Test
    public void testClassWithSuper() {

        final RefBookDataRequest emptyRequest = new RefBookDataRequest();
        final RefBookCreateRequest superRequest = new RefBookCreateRequest();
        assertNotEquals(emptyRequest, superRequest);

        final RefBookDataRequest newRequest = createRequest();
        final RefBookCreateRequest copyRequest = copySuperRequest(newRequest);
        assertObjects(Assert::assertNotEquals, newRequest, copyRequest);
    }

    private RefBookDataRequest createRequest() {

        final RefBookDataRequest request = new RefBookDataRequest();
        request.setCode("TEST");
        request.setPassport(new HashMap<>(1));
        request.getPassport().put("name", "Test");
        request.setStructure("{}");
        request.setData("{}");

        FileModel fileModel = new FileModel("filePath", "fileName");
        request.setFileModel(fileModel);

        return request;
    }

    private RefBookDataRequest copyRequest(RefBookDataRequest request) {

        final RefBookDataRequest result = new RefBookDataRequest();
        result.setCode(request.getCode());
        result.setType(request.getType());
        result.setPassport(request.getPassport());
        result.setStructure(request.getStructure());
        result.setData(request.getData());
        result.setFileModel(request.getFileModel());

        return result;
    }

    private RefBookCreateRequest copySuperRequest(RefBookCreateRequest request) {

        return new RefBookCreateRequest(request.getCode(), request.getType(),
                request.getCategory(), request.getPassport());
    }
}