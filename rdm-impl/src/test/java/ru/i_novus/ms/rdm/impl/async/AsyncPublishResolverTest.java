package ru.i_novus.ms.rdm.impl.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.async.AsyncOperationMessage;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.impl.BaseTest;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AsyncPublishResolverTest extends BaseTest {

    private static final User TEST_USER = new User("userId", "userName");

    private static final UUID OPERATION_ID = UUID.randomUUID();
    private static final AsyncOperationTypeEnum ASYNC_OPERATION_TYPE = AsyncOperationTypeEnum.PUBLICATION;

    private static final String REFBOOK_CODE = "test";
    private static final int REFBOOK_VERSION_ID = -10;

    @InjectMocks
    private AsyncPublishResolver resolver;

    @Mock
    private PublishService publishService;

    @Test
    public void testGetName() {

        assertEquals(AsyncPublishResolver.NAME, resolver.getName());
    }

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(ASYNC_OPERATION_TYPE));
        assertFalse(resolver.isSatisfied(null));
    } 

    @Test
    public void testResolve() {

        final AsyncOperationMessage message = new AsyncOperationMessage(
                OPERATION_ID, ASYNC_OPERATION_TYPE, REFBOOK_CODE,
                new Serializable[] {REFBOOK_VERSION_ID, new PublishRequest()}, TEST_USER
        );

        final Serializable result = resolver.resolve(message);
        assertNull(result);
    }

    @Test
    public void testResolveFailed() {

        final AsyncOperationMessage message = new AsyncOperationMessage(
                OPERATION_ID, ASYNC_OPERATION_TYPE, REFBOOK_CODE,
                new Serializable[] {REFBOOK_VERSION_ID, null}, TEST_USER
        );

        try {
            resolver.resolve(message);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }
}