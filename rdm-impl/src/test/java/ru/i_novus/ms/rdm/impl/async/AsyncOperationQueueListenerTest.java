package ru.i_novus.ms.rdm.impl.async;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.audit.client.model.User;
import ru.i_novus.ms.rdm.api.async.AsyncOperationHandler;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.i_novus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AsyncOperationQueueListenerTest {

    private static final String TEST_QUEUE_ID = "TEST_QUEUE_ID";
    private static final User TEST_USER = new User("userId", "userName");

    private static final UUID TEST_OPERATION_ID = UUID.randomUUID();
    private static final AsyncOperationTypeEnum TEST_ASYNC_OPERATION_TYPE = AsyncOperationTypeEnum.PUBLICATION;
    private static final String TEST_REFBOOK_CODE = "ref-book";
    private static final Integer TEST_REFBOOK_ID = 10;

    @InjectMocks
    private AsyncOperationQueueListener listener;

    @Mock
    private AsyncOperationLogEntryRepository repository;

    @Mock
    private AsyncOperationHandler handler;

    @Mock
    private Messages messages;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() throws NoSuchFieldException {

        FieldSetter.setField(listener, AsyncOperationQueueListener.class.getDeclaredField("queueId"), TEST_QUEUE_ID);
    }

    @Test
    public void testOnMessage() {

        AsyncOperationLogEntryEntity loadedEntity = new AsyncOperationLogEntryEntity();
        loadedEntity.setUuid(TEST_OPERATION_ID);
        loadedEntity.setOperationType(TEST_ASYNC_OPERATION_TYPE);
        loadedEntity.setCode(TEST_REFBOOK_CODE);

        when(repository.findByUuid(TEST_OPERATION_ID))
                .thenReturn(null)
                .thenReturn(loadedEntity);

        Serializable[] args = new Serializable[]{TEST_REFBOOK_ID};
        AsyncOperationMessage message = new AsyncOperationMessage(TEST_OPERATION_ID,
                TEST_ASYNC_OPERATION_TYPE, TEST_REFBOOK_CODE, args, TEST_USER);

        when(repository.save(any(AsyncOperationLogEntryEntity.class))).thenAnswer(v -> v.getArguments()[0]);

        listener.onMessage(message);

        ArgumentCaptor<AsyncOperationLogEntryEntity> logEntryCaptor = ArgumentCaptor.forClass(AsyncOperationLogEntryEntity.class);
        verify(repository, times(2)).save(logEntryCaptor.capture());

        AsyncOperationLogEntryEntity savedEntity = logEntryCaptor.getValue();
        assertEquals(loadedEntity.getUuid(), savedEntity.getUuid());
        assertEquals(loadedEntity.getOperationType(), savedEntity.getOperationType());
        assertEquals(loadedEntity.getCode(), savedEntity.getCode());
    }
}