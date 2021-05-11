package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.util.UnversionedVersionNumberStrategy;

import java.util.List;

@Component
public class UnversionedCreateDraftEntityStrategy extends DefaultCreateDraftEntityStrategy {

    @Autowired
    private UnversionedVersionNumberStrategy versionNumberStrategy;

    @Override
    protected RefBookVersionEntity createEntity(RefBookEntity refBookEntity, Structure structure,
                                                List<PassportValueEntity> passportValues) {

        RefBookVersionEntity entity = super.createEntity(refBookEntity, structure, passportValues);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setVersion(versionNumberStrategy.first());
        entity.setFromDate(TimeUtils.now());

        return entity;
    }
}
