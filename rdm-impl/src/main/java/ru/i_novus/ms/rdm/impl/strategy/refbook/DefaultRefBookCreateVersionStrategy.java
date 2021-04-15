package ru.i_novus.ms.rdm.impl.strategy.refbook;

import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity.toPassportValues;

public class DefaultRefBookCreateVersionStrategy implements RefBookCreateVersionStrategy {

    @Override
    public RefBookVersionEntity create(RefBookEntity refBookEntity, RefBookCreateRequest request) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setRefBook(refBookEntity);
        entity.setStatus(RefBookVersionStatus.DRAFT);

        if (request.getPassport() != null) {
            entity.setPassportValues(toPassportValues(request.getPassport(), false, entity));
        }

        entity.setStructure(new Structure());

        return entity;
    }
}
