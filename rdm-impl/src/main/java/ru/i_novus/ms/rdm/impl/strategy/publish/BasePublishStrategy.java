package ru.i_novus.ms.rdm.impl.strategy.publish;

import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface BasePublishStrategy extends Strategy {

    /**
     * Публикация версии справочника.
     *
     * @param entity  публикуемая версия
     * @param request параметры публикации
     * @return Результат публикации
     */
    PublishResponse publish(RefBookVersionEntity entity, PublishRequest request);
}
