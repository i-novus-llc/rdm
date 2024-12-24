package ru.i_novus.ms.rdm.async.impl.provider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class AsyncOperationQueueSenderTest {

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
    private AsyncOperationQueueSender sender;

    @Mock
    private JmsTemplate jmsTemplate;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {

        setField(sender, "queueId", QUEUE_ID);
    }

    @Test
    public void testSend() {

        final UUID actualId = sender.send(ASYNC_OPERATION_MESSAGE);
        assertEquals(OPERATION_ID, actualId);

        verify(jmsTemplate).convertAndSend(QUEUE_ID, ASYNC_OPERATION_MESSAGE);

        verifyNoMoreInteractions(jmsTemplate);
    }
}