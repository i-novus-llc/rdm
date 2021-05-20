package ru.i_novus.ms.rdm.impl.strategy.draft;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface ValidateDraftExistsStrategy extends Strategy {

    void validate(RefBookVersionEntity entity, Integer id);
}
