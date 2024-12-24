package ru.i_novus.ms.rdm.async.impl.provider;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationSender;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationSenderHandler;
import ru.i_novus.ms.rdm.impl.BaseTest;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class AsyncOperationSenderHandlerTest extends BaseTest {

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
    public void testHandleEmptySender() {

        final List<AsyncOperationSender> emptySenderList = emptyList();
        final AsyncOperationSenderHandler handler = new AsyncOperationSenderHandlerImpl(emptySenderList);

        final Map<String, UUID> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNull(map);
    }

    @Test
    public void testHandleSingleSender() {

        final List<AsyncOperationSender> singleSenderList = singletonList(
                new TestFirstPublicationSender()
        );
        final AsyncOperationSenderHandler handler = new AsyncOperationSenderHandlerImpl(singleSenderList);

        final Map<String, UUID> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNotNull(map);
        assertEquals(0,  map.size());
    }

    @Test
    public void testHandleDoubleSender() {

        final List<AsyncOperationSender> doubleSenderList = List.of(
                new TestFirstPublicationSender(),
                new TestSecondPublicationSender()
        );
        final AsyncOperationSenderHandler handler = new AsyncOperationSenderHandlerImpl(doubleSenderList);

        final Map<String, UUID> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNotNull(map);
        assertEquals(0,  map.size());
    }

    @Test
    public void testHandleNormalSender() {

        final List<AsyncOperationSender> senders = List.of(
                new TestFirstPublicationSender(),
                new TestSecondPublicationSender(),
                new TestNormalPublicationSender()
        );
        final AsyncOperationSenderHandler handler = new AsyncOperationSenderHandlerImpl(senders);

        final Map<String, UUID> map = handler.handle(ASYNC_OPERATION_MESSAGE);
        assertNotNull(map);
        assertEquals(1,  map.size());

        final Serializable result = map.values().iterator().next();
        assertEquals(OPERATION_ID,  result);
    }

    @Test
    public void testHandleThrownSender() {

        final List<AsyncOperationSender> senders = List.of(
                new TestFirstPublicationSender(),
                new TestSecondPublicationSender(),
                new TestThrownPublicationSender()
        );
        final AsyncOperationSenderHandler handler = new AsyncOperationSenderHandlerImpl(senders);

        try {
            handler.handle(ASYNC_OPERATION_MESSAGE);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(FAILURE_MESSAGE, getExceptionMessage(e));
        }
    }

    private static class TestFirstPublicationSender implements AsyncOperationSender {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public UUID send(AsyncOperationMessage message) {
            return null;
        }
    }

    private static class TestSecondPublicationSender implements AsyncOperationSender {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public UUID send(AsyncOperationMessage message) {
            return null;
        }
    }

    private static class TestNormalPublicationSender implements AsyncOperationSender {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public UUID send(AsyncOperationMessage message) {
            return OPERATION_ID;
        }
    }

    private static class TestThrownPublicationSender implements AsyncOperationSender {

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
            return ASYNC_OPERATION_TYPE.equals(operationType);
        }

        @Override
        public UUID send(AsyncOperationMessage message) {
            throw new IllegalArgumentException(FAILURE_MESSAGE);
        }
    }
}
