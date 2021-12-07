package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.AuditLogService;

import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus.PUBLISHED;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPublishEndStrategyTest {

    private static final int REFBOOK_ID = 2;
    private static final String REFBOOK_CODE = "refbook_code";

    private static final int VERSION_ID = 1;

    private static final String PUBLISH_TOPIC = "publishTopic";

    @InjectMocks
    private DefaultPublishEndStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private JmsTemplate jmsTemplate;

    @Before
    public void setUp() throws NoSuchFieldException {

        FieldSetter.setField(strategy, DefaultPublishEndStrategy.class.getDeclaredField("publishTopic"), PUBLISH_TOPIC);
        FieldSetter.setField(strategy, DefaultPublishEndStrategy.class.getDeclaredField("enablePublishTopic"), true);
    }

    @Test
    public void testApply() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(entity));

        PublishResponse response = new PublishResponse();
        response.setRefBookCode(REFBOOK_CODE);
        response.setNewId(entity.getId());

        strategy.apply(entity, response);

        ArgumentCaptor<Supplier> lambdaCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(auditLogService).addAction(eq(AuditAction.PUBLICATION), lambdaCaptor.capture());

        RefBookVersionEntity versionEntity = (RefBookVersionEntity) lambdaCaptor.getValue().get();
        assertSame(entity, versionEntity);

        verify(jmsTemplate).convertAndSend(PUBLISH_TOPIC, REFBOOK_CODE);
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REFBOOK_CODE);

        return entity;
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(VERSION_ID);
        entity.setStorageCode("published-storage-code");
        entity.setRefBook(createRefBookEntity());
        entity.setStatus(PUBLISHED);

        return entity;
    }
}