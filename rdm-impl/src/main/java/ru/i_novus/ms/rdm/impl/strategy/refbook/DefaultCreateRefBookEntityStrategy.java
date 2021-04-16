package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;

@Component
public class DefaultCreateRefBookEntityStrategy implements CreateRefBookEntityStrategy {

    @Override
    public RefBookEntity create(RefBookCreateRequest request) {

        RefBookEntity entity = new RefBookEntity();
        entity.setCode(request.getCode());
        entity.setType(request.getType() != null ? request.getType() : RefBookType.DEFAULT);
        entity.setArchived(Boolean.FALSE);
        entity.setRemovable(Boolean.TRUE);
        entity.setCategory(request.getCategory());

        return entity;
    }
}
