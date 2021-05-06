package ru.i_novus.ms.rdm.impl.strategy.draft;

import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface FindDraftEntityStrategy extends Strategy {

    RefBookVersionEntity find(RefBookEntity refBookEntity);
}
