package ru.i_novus.ms.rdm.impl.strategy.refbook;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

public class UnversionedCreateFirstVersionStrategy extends DefaultCreateFirstVersionStrategy {

    @Override
    public RefBookVersionEntity create(RefBookEntity refBookEntity, RefBookCreateRequest request) {

        RefBookVersionEntity entity = super.create(refBookEntity, request);
        entity.setVersion("1.0"); // from SequenceVersionNumberStrategy

        return entity;
    }
}
