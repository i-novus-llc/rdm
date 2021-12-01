package ru.i_novus.ms.rdm.impl.async;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;
import ru.i_novus.ms.audit.client.UserAccessor;
import ru.i_novus.ms.audit.client.model.User;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AsyncOperationQueueTest {

    private static final String TEST_QUEUE_ID = "TEST_QUEUE_ID";
    private static final User TEST_USER = new User("userId", "userName");

    private static final AsyncOperationTypeEnum TEST_ASYNC_OPERATION_TYPE = AsyncOperationTypeEnum.PUBLICATION;
    private static final String TEST_REFBOOK_CODE = "ref-book";
    private static final Integer TEST_REFBOOK_ID = 10;

    @InjectMocks
    private AsyncOperationQueue queue;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private AsyncOperationLogEntryRepository repository;

    @Mock
    private UserAccessor userAccessor;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() throws NoSuchFieldException {

        FieldSetter.setField(queue, AsyncOperationQueue.class.getDeclaredField("queueId"), TEST_QUEUE_ID);
    }

    @Test
    public void testAdd() {

        when(userAccessor.get()).thenReturn(TEST_USER);

        Serializable[] args = new Serializable[]{TEST_REFBOOK_ID};
        UUID result = queue.add(TEST_ASYNC_OPERATION_TYPE, TEST_REFBOOK_CODE, args);

        verify(userAccessor).get();

        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(repository).saveWithoutConflict(uuidCaptor.capture(), eq(TEST_ASYNC_OPERATION_TYPE.name()), eq(TEST_REFBOOK_CODE), any(String.class));

        assertEquals(result, uuidCaptor.getValue());

        verifyNoMore();
    }

    @Test
    public void testSend() {

        when(userAccessor.get()).thenReturn(TEST_USER);

        Serializable[] args = new Serializable[]{TEST_REFBOOK_ID};
        UUID result = queue.send(TEST_ASYNC_OPERATION_TYPE, TEST_REFBOOK_CODE, args);

        verify(userAccessor).get();

        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(repository).saveWithoutConflict(uuidCaptor.capture(), eq(TEST_ASYNC_OPERATION_TYPE.name()), eq(TEST_REFBOOK_CODE), any(String.class));

        verify(jmsTemplate).convertAndSend(eq(TEST_QUEUE_ID), any(AsyncOperationMessage.class));

        assertEquals(result, uuidCaptor.getValue());

        verifyNoMore();
    }

    private void verifyNoMore() {

        verifyNoMoreInteractions(jmsTemplate, repository, userAccessor);
    }
}