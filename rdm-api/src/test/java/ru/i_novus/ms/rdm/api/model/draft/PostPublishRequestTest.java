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

        PostPublishRequest cloneRequest = cloneRequest(newRequest);
        assertObjects(Assert::assertEquals, newRequest, cloneRequest);
        
        PostPublishRequest copyRequest = copyRequest(newRequest);
        assertObjects(Assert::assertEquals, newRequest, copyRequest);
    }

    private PostPublishRequest createRequest() {

        PostPublishRequest request = new PostPublishRequest();

        request.setLastStorageCode("last-storage-code");
        request.setOldStorageCode("old-storage-code");
        request.setNewStorageCode("new-storage-code");

        request.setFromDate(LocalDateTime.now());
        request.setToDate(LocalDateTime.now());

        return request;
    }

    private PostPublishRequest cloneRequest(PostPublishRequest criteria) {

        return new PostPublishRequest(criteria.getLastStorageCode(),
                criteria.getOldStorageCode(), criteria.getNewStorageCode(),
                criteria.getFromDate(), criteria.getToDate());
    }

    private PostPublishRequest copyRequest(PostPublishRequest criteria) {

        PostPublishRequest result = new PostPublishRequest();

        result.setLastStorageCode(criteria.getLastStorageCode());
        result.setOldStorageCode(criteria.getOldStorageCode());
        result.setNewStorageCode(criteria.getNewStorageCode());

        result.setFromDate(criteria.getFromDate());
        result.setToDate(criteria.getToDate());

        return result;
    }
}