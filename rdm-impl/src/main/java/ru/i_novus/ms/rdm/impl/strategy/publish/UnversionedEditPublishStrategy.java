package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

/**
 * Публикация неверсионного справочника при изменениях в нём.
 */
@Component
public class UnversionedEditPublishStrategy implements EditPublishStrategy {

    @Autowired
    @Qualifier("unversionedBasePublishStrategy")
    private BasePublishStrategy basePublishStrategy;

    @Override
    @Transactional
    public PublishResponse publish(RefBookVersionEntity entity) {

        PublishRequest request = new PublishRequest();
        request.setOptLockValue(entity.getOptLockValue());
        request.setResolveConflicts(false);

        return basePublishStrategy.publish(entity, request);
    }
}
