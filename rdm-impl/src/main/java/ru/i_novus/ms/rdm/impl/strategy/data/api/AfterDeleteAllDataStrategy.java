package ru.i_novus.ms.rdm.impl.strategy.data.api;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface AfterDeleteAllDataStrategy extends Strategy {

    void apply(RefBookVersionEntity entity);
}
