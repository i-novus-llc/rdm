package ru.i_novus.ms.rdm.rest.async;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.i_novus.ms.rdm.api.async.AsyncOperationMessage;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.api.service.async.AsyncOperationMessageService;

import java.io.Serializable;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class AsyncOperationQueueListenerTest {

    private static final String QUEUE_ID = "TEST_QUEUE_ID";
    private static final User TEST_USER = new User("userId", "userName");

    private static final UUID OPERATION_ID = UUID.randomUUID();
    private static final AsyncOperationTypeEnum ASYNC_OPERATION_TYPE = AsyncOperationTypeEnum.PUBLICATION;

    private static final String REFBOOK_CODE = "ref-book";
    private static final Integer REFBOOK_VERSION_ID = -10;

    private static final Serializable[] HANDLER_ARGS = {REFBOOK_VERSION_ID};
    private static final AsyncOperationMessage ASYNC_OPERATION_MESSAGE = new AsyncOperationMessage(
            OPERATION_ID, ASYNC_OPERATION_TYPE, REFBOOK_CODE, HANDLER_ARGS, TEST_USER
    );

    @InjectMocks
    private AsyncOperationQueueListener listener;

    @Mock
    private AsyncOperationMessageService service;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {

        setField(listener, "queueId", QUEUE_ID);
    }

    @Test
    public void testOnMessage() {

        final AsyncOperationLogEntry model = new AsyncOperationLogEntry();
        model.setId(OPERATION_ID);
        model.setOperationType(ASYNC_OPERATION_TYPE);
        model.setCode(REFBOOK_CODE);

        when(service.receive(ASYNC_OPERATION_MESSAGE)).thenReturn(model);

        listener.onMessage(ASYNC_OPERATION_MESSAGE);

        verify(service).receive(ASYNC_OPERATION_MESSAGE);
    }
}