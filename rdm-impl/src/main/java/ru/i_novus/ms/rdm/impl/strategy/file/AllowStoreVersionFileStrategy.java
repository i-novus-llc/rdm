package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface AllowStoreVersionFileStrategy extends Strategy {

    boolean allow(RefBookVersionEntity entity);
}
