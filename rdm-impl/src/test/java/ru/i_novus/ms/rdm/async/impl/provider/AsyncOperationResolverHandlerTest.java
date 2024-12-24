package ru.i_novus.ms.rdm.async.impl.provider;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationResolver;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationResolverHandler;
import ru.i_novus.ms.rdm.impl.BaseTest;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class AsyncOperationResolverHandlerTest extends BaseTest {

    private static final UUID OPERATION_ID = UUID.randomUUID();
    private static final AsyncOperationTypeEnum ASYNC_OPERATION_TYPE = AsyncOperationTypeEnum.PUBLICATION;
    private static final User TEST_USER = new User("userId", "userName");

    private static final String REFBOOK_CODE = "test";
    private static final int REFBOOK_VERSION_ID = -10;

    private static final Serializable[] HANDLER_ARGS = {REFBOOK_VERSION_ID};
    private static final AsyncOperationMessage ASYNC_OPERATION_MESSAGE = new AsyncOperationMessage(
            OPERATION_ID, ASYNC_OPERATION_TYPE, REFBOOK_CODE, HANDLER_ARGS, TEST_USER
    );

    private static final String SUCCESS_RESULT = "ok";
    private static final String FAILURE_MESSAGE = "error";

    @Test
    public void testHandleEmptyResolver() {

        final List<AsyncOperationResolver> emptyResolverList = emptyList();
        final AsyncOperationResolverHandler handler = new AsyncOperationResolverHandlerImpl(emptyResolverList);

        final Map<String, Serializable> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNull(map);
    }

    @Test
    public void testHandleSingleResolver() {

        final List<AsyncOperationResolver> singleResolverList = singletonList(
                new TestFirstPublicationResolver()
        );
        final AsyncOperationResolverHandler handler = new AsyncOperationResolverHandlerImpl(singleResolverList);

        final Map<String, Serializable> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNotNull(map);
        assertEquals(0,  map.size());
    }

    @Test
    public void testHandleDoubleResolver() {

        final List<AsyncOperationResolver> doubleResolverList = List.of(
                new TestFirstPublicationResolver(),
                new TestSecondPublicationResolver()
        );
        final AsyncOperationResolverHandler handler = new AsyncOperationResolverHandlerImpl(doubleResolverList);

        final Map<String, Serializable> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNotNull(map);
        assertEquals(0,  map.size());
    }

    @Test
    public void testHandleNormalResolver() {

        final List<AsyncOperationResolver> resolvers = List.of(
                new TestFirstPublicationResolver(),
                new TestSecondPublicationResolver(),
                new TestNormalPublicationResolver()
        );
        final AsyncOperationResolverHandler handler = new AsyncOperationResolverHandlerImpl(resolvers);

        final Map<String, Serializable> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNotNull(map);
        assertEquals(1,  map.size());

        final Serializable result = map.values().iterator().next();
        assertEquals(SUCCESS_RESULT,  result);
    }

    @Test
    public void testHandleThrownResolver() {

        final List<AsyncOperationResolver> resolvers = List.of(
                new TestFirstPublicationResolver(),
                new TestSecondPublicationResolver(),
                new TestThrownPublicationResolver()
        );
        final AsyncOperationResolverHandler handler = new AsyncOperationResolverHandlerImpl(resolvers);

        try {
            handler.handle(ASYNC_OPERATION_MESSAGE);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(FAILURE_MESSAGE, getExceptionMessage(e));
        }
    }

    private static class TestFirstPublicationResolver implements AsyncOperationResolver {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public Serializable resolve(AsyncOperationMessage message) {
            return null;
        }
    }

    private static class TestSecondPublicationResolver implements AsyncOperationResolver {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public Serializable resolve(AsyncOperationMessage message) {
            return null;
        }
    }

    private static class TestNormalPublicationResolver implements AsyncOperationResolver {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public Serializable resolve(AsyncOperationMessage message) {
            return SUCCESS_RESULT;
        }
    }

    private static class TestThrownPublicationResolver implements AsyncOperationResolver {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public Serializable resolve(AsyncOperationMessage message) {
            throw new IllegalArgumentException(FAILURE_MESSAGE);
        }
    }
}
