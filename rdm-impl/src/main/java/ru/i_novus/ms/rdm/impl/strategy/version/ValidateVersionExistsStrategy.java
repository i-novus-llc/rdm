package ru.i_novus.ms.rdm.impl.strategy.version;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface ValidateVersionExistsStrategy extends Strategy {

    void validate(RefBookVersionEntity entity, Integer id);
}
