package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.api.util.VersionNumberStrategy;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedCreateFirstVersionStrategy extends DefaultCreateFirstVersionStrategy {

    @Autowired
    private VersionNumberStrategy versionNumberStrategy;

    @Override
    protected RefBookVersionEntity createEntity(RefBookCreateRequest request,
                                                RefBookEntity refBookEntity,
                                                String storageCode) {

        RefBookVersionEntity entity = super.createEntity(request, refBookEntity, storageCode);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setVersion(versionNumberStrategy.first());
        entity.setFromDate(TimeUtils.now());

        return entity;
    }
}
