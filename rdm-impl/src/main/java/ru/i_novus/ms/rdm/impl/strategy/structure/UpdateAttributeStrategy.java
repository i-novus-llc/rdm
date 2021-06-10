package ru.i_novus.ms.rdm.impl.strategy.structure;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface UpdateAttributeStrategy extends Strategy {

    Structure.Attribute update(RefBookVersionEntity entity, UpdateAttributeRequest request);
}
