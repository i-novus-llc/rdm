package ru.i_novus.ms.rdm.impl.strategy.refbook;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface RefBookCreateEntityStrategy extends Strategy {

    RefBookEntity create(RefBookCreateRequest request);
}
