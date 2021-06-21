package ru.i_novus.ms.rdm.impl.strategy.data;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

import java.util.List;

public interface DeleteRowValuesStrategy extends Strategy {

    void delete(RefBookVersionEntity entity, List<Object> systemIds);
}
