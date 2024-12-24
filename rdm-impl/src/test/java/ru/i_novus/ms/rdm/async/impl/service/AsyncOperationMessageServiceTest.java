package ru.i_novus.ms.rdm.async.impl.service;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.audit.UserAccessor;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationResolverHandler;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationSenderHandler;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.i_novus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class AsyncOperationMessageServiceTest {

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
    private AsyncOperationMessageServiceImpl service;

    @Mock
    private AsyncOperationLogEntryRepository repository;

    @Mock
    private AsyncOperationSenderHandler senderHandler;

    @Mock
    private AsyncOperationResolverHandler resolverHandler;

    @Mock
    private UserAccessor userAccessor;

    @Mock
    private Messages messages;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {

        setField(service, "objectMapper", JsonUtil.getMapper());
    }

    @Test
    public void testCreate() {

        when(userAccessor.get()).thenReturn(TEST_USER);

        final AsyncOperationMessage result = service.create(ASYNC_OPERATION_TYPE, REFBOOK_CODE, HANDLER_ARGS);

        verify(userAccessor).get();

        final UUID operationId = verifySaveWithoutConflict();
        assertEquals(result.getOperationId(), operationId);

        verifyNoMore();
    }

    @Test
    public void testSend() {

        when(userAccessor.get()).thenReturn(TEST_USER);

        final UUID result = service.send(ASYNC_OPERATION_TYPE, REFBOOK_CODE, HANDLER_ARGS);

        verify(userAccessor).get();

        final UUID operationId = verifySaveWithoutConflict();
        assertEquals(result, operationId);

        final ArgumentCaptor<AsyncOperationMessage> messageCaptor = ArgumentCaptor.forClass(AsyncOperationMessage.class);
        verify(senderHandler).handle(messageCaptor.capture());
        assertEquals(result, messageCaptor.getValue().getOperationId());

        verifyNoMore();
    }

    @Test
    public void testReceive() {

        final AsyncOperationLogEntryEntity entity = new AsyncOperationLogEntryEntity();
        entity.setUuid(OPERATION_ID);
        entity.setOperationType(ASYNC_OPERATION_TYPE);
        entity.setCode(REFBOOK_CODE);

        when(repository.findByUuid(OPERATION_ID))
                .thenReturn(null)
                .thenReturn(entity);

        when(repository.save(any(AsyncOperationLogEntryEntity.class))).thenAnswer(v -> v.getArguments()[0]);

        final AsyncOperationLogEntry model = service.receive(ASYNC_OPERATION_MESSAGE);
        assertEquals(entity.getUuid(), model.getId());
        assertEquals(entity.getOperationType(), model.getOperationType());
        assertEquals(entity.getCode(), model.getCode());

        verify(repository, times(2)).findByUuid(OPERATION_ID);

        final UUID operationId = verifySaveWithoutConflict();
        assertEquals(OPERATION_ID, operationId);

        verify(repository, times(2)).save(any(AsyncOperationLogEntryEntity.class));

        verify(resolverHandler).handle(ASYNC_OPERATION_MESSAGE);

        verifyNoMore();
    }

    private UUID verifySaveWithoutConflict() {

        final ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(repository).saveWithoutConflict(
                uuidCaptor.capture(), eq(ASYNC_OPERATION_TYPE.name()), eq(REFBOOK_CODE), any(String.class)
        );
        assertNotNull(uuidCaptor);

        return uuidCaptor.getValue();
    }

    private void verifyNoMore() {

        verifyNoMoreInteractions(repository, senderHandler, resolverHandler, userAccessor, messages);
    }
}