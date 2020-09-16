package ru.i_novus.ms.rdm.api.async;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.util.RefBookTestUtils.getExceptionMessage;
import static ru.i_novus.ms.rdm.api.util.RefBookTestUtils.getFailedMessage;

public class AsyncOperationHandlerTest {

    private static final String TEST_REF_BOOK_CODE = "test";
    private static final int TEST_REFBOOK_VERSION_ID = -10;

    private static final String SUCCESS_RESULT = "ok";
    private static final String FAILURE_MESSAGE = "error";

    @Test
    public void testHandleEmptyResolver() {

        final List<AsyncOperationResolver> emptyResolverList = emptyList();

        AsyncOperationHandler handler = new AsyncOperationHandler(emptyResolverList);

        try {
            handler.handle(AsyncOperationTypeEnum.L10N_PUBLICATION, TEST_REF_BOOK_CODE,
                    new Serializable[]{TEST_REFBOOK_VERSION_ID});
            fail(getFailedMessage(NotFoundException.class));

        } catch (UserException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }

    @Test
    public void testHandleSingleResolver() {

        final List<AsyncOperationResolver> singleResolverList = singletonList(
                new TestFirstPublicationResolver()
        );

        AsyncOperationHandler handler = new AsyncOperationHandler(singleResolverList);

        Serializable result = handler.handle(AsyncOperationTypeEnum.PUBLICATION, TEST_REF_BOOK_CODE,
                new Serializable[]{TEST_REFBOOK_VERSION_ID});
        assertNull(result);
    }

    @Test
    public void testHandleDoubleResolver() {

        final List<AsyncOperationResolver> doubleResolverList = List.of(
                new TestFirstPublicationResolver(),
                new TestSecondPublicationResolver()
        );

        AsyncOperationHandler handler = new AsyncOperationHandler(doubleResolverList);

        Serializable result = handler.handle(AsyncOperationTypeEnum.PUBLICATION, TEST_REF_BOOK_CODE,
                new Serializable[]{TEST_REFBOOK_VERSION_ID});
        assertTrue(result instanceof List);
        assertEquals(0,  ((List) result).size());
    }

    @Test
    public void testHandleNormalResolver() {

        final List<AsyncOperationResolver> resolvers = List.of(
                new TestFirstPublicationResolver(),
                new TestSecondPublicationResolver(),
                new TestNormalPublicationResolver()
        );

        AsyncOperationHandler handler = new AsyncOperationHandler(resolvers);

        Serializable result = handler.handle(AsyncOperationTypeEnum.L10N_PUBLICATION, TEST_REF_BOOK_CODE,
                new Serializable[]{TEST_REFBOOK_VERSION_ID});
        assertNotNull(result);
        assertEquals(SUCCESS_RESULT,  result);
    }

    @Test
    public void testHandleThrownResolver() {

        final List<AsyncOperationResolver> resolvers = List.of(
                new TestFirstPublicationResolver(),
                new TestSecondPublicationResolver(),
                new TestThrownPublicationResolver()
        );

        AsyncOperationHandler handler = new AsyncOperationHandler(resolvers);

        try {
            handler.handle(AsyncOperationTypeEnum.L10N_PUBLICATION, TEST_REF_BOOK_CODE,
                    new Serializable[]{TEST_REFBOOK_VERSION_ID});
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals(FAILURE_MESSAGE, getExceptionMessage(e));
        }
    }

    private class TestFirstPublicationResolver implements AsyncOperationResolver {

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return AsyncOperationTypeEnum.PUBLICATION.equals(operationType);
        }

        @Override
        public Serializable resolve(String code, Serializable[] args) {
            return null;
        }
    }

    private class TestSecondPublicationResolver implements AsyncOperationResolver {

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return AsyncOperationTypeEnum.PUBLICATION.equals(operationType);
        }

        @Override
        public Serializable resolve(String code, Serializable[] args) {
            return null;
        }
    }

    private class TestNormalPublicationResolver implements AsyncOperationResolver {

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return AsyncOperationTypeEnum.L10N_PUBLICATION.equals(operationType);
        }

        @Override
        public Serializable resolve(String code, Serializable[] args) {
            return SUCCESS_RESULT;
        }
    }

    private class TestThrownPublicationResolver implements AsyncOperationResolver {

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return AsyncOperationTypeEnum.L10N_PUBLICATION.equals(operationType);
        }

        @Override
        public Serializable resolve(String code, Serializable[] args) {
            throw new IllegalArgumentException(FAILURE_MESSAGE);
        }
    }
}
