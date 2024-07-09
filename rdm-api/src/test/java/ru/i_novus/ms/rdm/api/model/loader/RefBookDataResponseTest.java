package ru.i_novus.ms.rdm.api.model.loader;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;

import java.time.LocalDateTime;

public class RefBookDataResponseTest extends BaseTest {

    @Test
    public void testClass() {

        final RefBookDataResponse emptyResponse = new RefBookDataResponse();
        assertSpecialEquals(emptyResponse);

        final RefBookDataResponse newResponse = createResponse();
        assertObjects(Assert::assertNotEquals, emptyResponse, newResponse);

        final RefBookDataResponse copyResponse = copyResponse(newResponse);
        assertObjects(Assert::assertEquals, newResponse, copyResponse);
    }

    private RefBookDataResponse createResponse() {

        final RefBookDataResponse result = new RefBookDataResponse();
        result.setRefBookId(1);
        result.setExecutedDate(LocalDateTime.now());

        return result;
    }

    private RefBookDataResponse copyResponse(RefBookDataResponse response) {

        final RefBookDataResponse result = new RefBookDataResponse();
        result.setRefBookId(response.getRefBookId());
        result.setExecutedDate(response.getExecutedDate());

        return result;
    }
}