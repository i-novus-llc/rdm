package ru.i_novus.ms.rdm.impl.strategy.structure;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface DeleteAttributeStrategy extends Strategy {

    Structure.Attribute delete(RefBookVersionEntity entity, DeleteAttributeRequest request);
}
