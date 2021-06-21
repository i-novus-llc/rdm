package ru.i_novus.ms.rdm.impl.strategy.structure;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface CreateAttributeStrategy extends Strategy {

    Structure.Attribute create(RefBookVersionEntity entity, CreateAttributeRequest request);
}
