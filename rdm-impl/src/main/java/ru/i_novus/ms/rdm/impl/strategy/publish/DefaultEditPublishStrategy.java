package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

/**
 * Публикация версионного справочника при изменениях в нём.
 * Не выполняется, т.к. для публикации есть специальное действие "Опубликовать".
 */
@Component
public class DefaultEditPublishStrategy implements EditPublishStrategy {

    @Override
    public PublishResponse publish(RefBookVersionEntity entity) {
        return new PublishResponse(); // Nothing to do.
    }
}
