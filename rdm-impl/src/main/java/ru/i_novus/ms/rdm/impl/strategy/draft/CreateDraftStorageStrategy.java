package ru.i_novus.ms.rdm.impl.strategy.draft;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface CreateDraftStorageStrategy extends Strategy {

    String create(Structure structure);
}
