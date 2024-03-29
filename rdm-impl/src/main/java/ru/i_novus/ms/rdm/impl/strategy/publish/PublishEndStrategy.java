package ru.i_novus.ms.rdm.impl.strategy.publish;

import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

/**
 * Стратегия для действий в конце публикации справочника.
 */
public interface PublishEndStrategy extends Strategy {

    /**
     * Выполнение действий в конце публикации версии справочника.
     *
     * @param entity   публикуемая версия
     * @param response результат публикации
     */
    void apply(RefBookVersionEntity entity, PublishResponse response);
}
