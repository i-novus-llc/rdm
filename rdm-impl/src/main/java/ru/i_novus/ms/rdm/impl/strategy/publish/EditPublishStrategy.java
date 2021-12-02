package ru.i_novus.ms.rdm.impl.strategy.publish;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface EditPublishStrategy extends Strategy {

    void publish(RefBookVersionEntity entity);
}
