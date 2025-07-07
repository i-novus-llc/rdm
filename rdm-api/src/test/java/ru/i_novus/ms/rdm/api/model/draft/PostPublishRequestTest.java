package ru.i_novus.ms.rdm.api.model.draft;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;

import java.time.LocalDateTime;

public class PostPublishRequestTest extends BaseTest {

    @Test
    public void testClass() {

        PostPublishRequest emptyRequest = new PostPublishRequest();
        assertSpecialEquals(emptyRequest);

        PostPublishRequest newRequest = createRequest();
        assertObjects(Assert::assertNotEquals, emptyRequest, newRequest);

        PostPublishRequest copyRequest = copyRequest(newRequest);
        assertObjects(Assert::assertEquals, newRequest, copyRequest);
    }

    private PostPublishRequest createRequest() {

        final PostPublishRequest request = new PostPublishRequest();
        request.setRefBookCode("ref_book_code");

        request.setLastStorageCode("last-storage-code");
        request.setOldStorageCode("old-storage-code");
        request.setNewStorageCode("new-storage-code");

        request.setFromDate(LocalDateTime.now());
        request.setToDate(LocalDateTime.now());

        return request;
    }

    private PostPublishRequest copyRequest(PostPublishRequest criteria) {

        final PostPublishRequest result = new PostPublishRequest();
        result.setRefBookCode(criteria.getRefBookCode());

        result.setLastStorageCode(criteria.getLastStorageCode());
        result.setOldStorageCode(criteria.getOldStorageCode());
        result.setNewStorageCode(criteria.getNewStorageCode());

        result.setFromDate(criteria.getFromDate());
        result.setToDate(criteria.getToDate());

        return result;
    }
}