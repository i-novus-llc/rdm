package ru.i_novus.ms.rdm.l10n.impl.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.l10n.impl.BaseTest;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;

import java.io.Serializable;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AsyncL10nPublishResolverTest extends BaseTest {

    private static final String TEST_REFBOOK_CODE = "test";

    @InjectMocks
    private AsyncL10nPublishResolver resolver;

    @Mock
    private L10nDraftDataService draftDataService;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(AsyncOperationTypeEnum.L10N_PUBLICATION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testResolve() {

        Serializable result = resolver.resolve(TEST_REFBOOK_CODE, new Serializable[]{new PostPublishRequest()});
        assertNull(result);
    }

    @Test
    public void testResolveFailed() {

        try {
            resolver.resolve(TEST_REFBOOK_CODE, new Serializable[]{null});
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }
}