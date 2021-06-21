package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;

@Component
public class DefaultCreateRefBookEntityStrategy implements CreateRefBookEntityStrategy {

    @Autowired
    private RefBookRepository refBookRepository;

    @Override
    public RefBookEntity create(RefBookCreateRequest request) {

        RefBookEntity entity = newEntity();
        fillEntity(entity, request);
        return saveEntity(entity);
    }

    protected void fillEntity(RefBookEntity entity, RefBookCreateRequest request) {

        entity.setCode(request.getCode());
        entity.setArchived(Boolean.FALSE);
        entity.setRemovable(Boolean.TRUE);
        entity.setCategory(request.getCategory());
    }

    protected RefBookEntity newEntity() {
        return new DefaultRefBookEntity();
    }

    protected RefBookEntity saveEntity(RefBookEntity entity) {
        return refBookRepository.save(entity);
    }
}
