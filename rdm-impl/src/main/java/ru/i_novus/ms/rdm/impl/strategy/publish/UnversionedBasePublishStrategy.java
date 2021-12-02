package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedBasePublishStrategy implements BasePublishStrategy {

    @Override
    @Transactional
    public PublishResponse publish(RefBookVersionEntity entity, PublishRequest request) {

        // todo: Обновить дату публикации справочника!

        return null;
    }
}
