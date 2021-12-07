package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.AuditLogService;

@Component
public class DefaultPublishEndStrategy implements PublishEndStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired(required = false)
    @Qualifier("topicJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Value("${rdm.publish.topic:publish_topic}")
    private String publishTopic;

    @Value("${rdm.enable.publish.topic:false}")
    private boolean enablePublishTopic;

    @Override
    public void apply(RefBookVersionEntity entity, PublishResponse response) {

        // В аудите подтягиваются значения паспорта справочника (а у них lazy-инициализация),
        // поэтому нужна транзакция (которой в этом методе нет) для их получения.
        auditLogService.addAction(AuditAction.PUBLICATION, () -> versionRepository.findById(entity.getId()).orElse(null));

        if (enablePublishTopic) {
            jmsTemplate.convertAndSend(publishTopic, entity.getRefBook().getCode());
        }
    }
}
