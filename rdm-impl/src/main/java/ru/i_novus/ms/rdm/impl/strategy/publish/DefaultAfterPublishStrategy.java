package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.service.AuditLogService;

@Component
public class DefaultAfterPublishStrategy implements AfterPublishStrategy {

    private final AuditLogService auditLogService;

    private final JmsTemplate jmsTemplate;

    @Value("${rdm.publish.topic:publish_topic}")
    private String publishTopic;

    @Value("${rdm.enable.publish.topic:false}")
    private boolean enablePublishTopic;

    @Autowired
    public DefaultAfterPublishStrategy(
            AuditLogService auditLogService,
            @Qualifier("topicJmsTemplate") @Autowired(required = false) JmsTemplate jmsTemplate
    ) {
        this.auditLogService = auditLogService;
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void apply(RefBookVersionEntity entity, PublishResponse response) {

        auditLogService.addAction(AuditAction.PUBLICATION, () -> entity);

        if (enablePublishTopic) {
            jmsTemplate.convertAndSend(publishTopic, entity.getRefBook().getCode());
        }
    }
}
