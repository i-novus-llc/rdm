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
    public RefBookVersionEntity create(RefBookEntity refBookEntity, RefBookCreateRequest request) {

        RefBookVersionEntity entity = super.create(refBookEntity, request);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setVersion(versionNumberStrategy.first());
        entity.setFromDate(TimeUtils.now());

        return entity;
    }
}
