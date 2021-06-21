package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import static ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity.toPassportValues;

@Component
public class DefaultCreateFirstVersionStrategy implements CreateFirstVersionStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Override
    public RefBookVersionEntity create(RefBookCreateRequest request,
                                       RefBookEntity refBookEntity,
                                       String storageCode) {

        RefBookVersionEntity entity = createEntity(request, refBookEntity, storageCode);
        return saveEntity(entity);
    }

    protected RefBookVersionEntity createEntity(RefBookCreateRequest request,
                                                RefBookEntity refBookEntity,
                                                String storageCode) {

        RefBookVersionEntity entity = refBookEntity.createChangeableVersion();

        if (request.getPassport() != null) {
            entity.setPassportValues(toPassportValues(request.getPassport(), false, entity));
        }

        entity.setStructure(new Structure());
        entity.setStorageCode(storageCode);

        return entity;
    }

    protected RefBookVersionEntity saveEntity(RefBookVersionEntity entity) {
        return versionRepository.save(entity);
    }
}
