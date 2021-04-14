package ru.i_novus.ms.rdm.impl.strategy.refbook;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;

public class DefaultRefBookCreateEntityStrategy implements RefBookCreateEntityStrategy {

    @Override
    public RefBookEntity create(RefBookCreateRequest request) {

        RefBookEntity entity = new RefBookEntity();
        entity.setCode(request.getCode());
        entity.setType(request.getType());
        entity.setArchived(Boolean.FALSE);
        entity.setRemovable(Boolean.TRUE);
        entity.setCategory(request.getCategory());

        return entity;
    }
}
