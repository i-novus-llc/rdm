package ru.i_novus.ms.rdm.impl.strategy.data.api;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

import java.util.List;

public interface AfterDeleteDataStrategy extends Strategy {

    void apply(RefBookVersionEntity entity, List<Object> systemIds);
}
