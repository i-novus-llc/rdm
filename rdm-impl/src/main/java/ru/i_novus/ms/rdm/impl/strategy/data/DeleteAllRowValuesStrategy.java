package ru.i_novus.ms.rdm.impl.strategy.data;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface DeleteAllRowValuesStrategy extends Strategy {

    void delete(RefBookVersionEntity entity);
}
