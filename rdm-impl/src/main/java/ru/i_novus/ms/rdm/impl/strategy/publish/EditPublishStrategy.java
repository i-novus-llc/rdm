package ru.i_novus.ms.rdm.impl.strategy.publish;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

/**
 * Стратегия публикации при изменениях справочника.
 */
public interface EditPublishStrategy extends Strategy {

    void publish(RefBookVersionEntity entity);
}
