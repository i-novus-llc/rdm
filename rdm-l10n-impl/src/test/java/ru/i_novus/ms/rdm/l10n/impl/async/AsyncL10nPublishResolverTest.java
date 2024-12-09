package ru.i_novus.ms.rdm.l10n.impl.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.async.AsyncOperationMessage;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.l10n.impl.BaseTest;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AsyncL10nPublishResolverTest extends BaseTest {

    private static final User TEST_USER = new User("userId", "userName");

    private static final String REFBOOK_CODE = "test";

    private static final UUID OPERATION_ID = UUID.randomUUID();
    private static final AsyncOperationTypeEnum ASYNC_OPERATION_TYPE = AsyncOperationTypeEnum.L10N_PUBLICATION;

    @InjectMocks
    private AsyncL10nPublishResolver resolver;

    @Mock
    private L10nDraftDataService draftDataService;

    @Test
    public void testGetName() {

        assertEquals(AsyncL10nPublishResolver.NAME, resolver.getName());
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
                new Serializable[] {new PostPublishRequest()}, TEST_USER
        );

        final Serializable result = resolver.resolve(message);
        assertNull(result);
    }

    @Test
    public void testResolveFailed() {

        final AsyncOperationMessage message = new AsyncOperationMessage(
                OPERATION_ID, ASYNC_OPERATION_TYPE, REFBOOK_CODE,
                new Serializable[] {null}, TEST_USER
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