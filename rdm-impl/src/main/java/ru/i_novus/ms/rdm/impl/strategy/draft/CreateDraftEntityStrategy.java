package ru.i_novus.ms.rdm.impl.strategy.draft;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

import java.util.List;

public interface CreateDraftEntityStrategy extends Strategy {

    RefBookVersionEntity create(RefBookEntity refBookEntity, Structure structure,
                                List<PassportValueEntity> passportValues);
}
