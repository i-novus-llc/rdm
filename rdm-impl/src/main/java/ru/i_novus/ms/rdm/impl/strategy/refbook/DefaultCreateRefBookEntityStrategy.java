package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;

@Component
public class DefaultCreateRefBookEntityStrategy implements CreateRefBookEntityStrategy {

    @Autowired
    private RefBookRepository refBookRepository;

    @Override
    public RefBookEntity create(RefBookCreateRequest request) {

        RefBookEntity entity = toEntity(request);
        return saveEntity(entity);
    }

    protected RefBookEntity toEntity(RefBookCreateRequest request) {

        RefBookEntity entity = new RefBookEntity();
        entity.setCode(request.getCode());
        entity.setType(request.getType() != null ? request.getType() : RefBookTypeEnum.DEFAULT);
        entity.setArchived(Boolean.FALSE);
        entity.setRemovable(Boolean.TRUE);
        entity.setCategory(request.getCategory());

        return entity;
    }

    protected RefBookEntity saveEntity(RefBookEntity entity) {
        return refBookRepository.save(entity);
    }
}
