package ru.i_novus.ms.rdm.impl.strategy.data.api;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface BeforeDeleteAllDataStrategy extends Strategy {

    void apply(RefBookVersionEntity entity);
}
