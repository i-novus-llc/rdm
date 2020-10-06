package ru.i_novus.ms.rdm.impl.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;

import java.io.Serializable;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.impl.util.RefBookTestUtils.getExceptionMessage;
import static ru.i_novus.ms.rdm.impl.util.RefBookTestUtils.getFailedMessage;

@RunWith(MockitoJUnitRunner.class)
public class AsyncPublishResolverTest {

    private static final String TEST_REFBOOK_CODE = "test";
    private static final int TEST_REFBOOK_VERSION_ID = -10;

    @InjectMocks
    private AsyncPublishResolver resolver;

    @Mock
    private PublishService publishService;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(AsyncOperationTypeEnum.PUBLICATION));
        assertFalse(resolver.isSatisfied(null));
    } 

    @Test
    public void testResolve() {

        Serializable result = resolver.resolve(TEST_REFBOOK_CODE,
                new Serializable[]{TEST_REFBOOK_VERSION_ID, new PublishRequest()}
        );
        assertNull(result);
    }

    @Test
    public void testResolveFailed() {

        try {
            resolver.resolve(TEST_REFBOOK_CODE, new Serializable[]{TEST_REFBOOK_VERSION_ID, null});
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }
}