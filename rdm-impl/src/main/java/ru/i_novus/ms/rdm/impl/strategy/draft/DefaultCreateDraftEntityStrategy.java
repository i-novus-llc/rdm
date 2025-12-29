package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import java.util.List;

import static ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity.copyPassportValues;

@Component
public class DefaultCreateDraftEntityStrategy implements CreateDraftEntityStrategy {

    @Override
    public RefBookVersionEntity create(RefBookEntity refBookEntity, Structure structure,
                                       List<PassportValueEntity> passportValues) {

        return createEntity(refBookEntity, structure, passportValues);
    }

    protected RefBookVersionEntity createEntity(RefBookEntity refBookEntity,
                                                Structure structure,
                                                List<PassportValueEntity> passportValues) {

        RefBookVersionEntity entity = refBookEntity.createChangeableVersion();
        entity.setStructure(structure);

        if (passportValues != null) {
            entity.setPassportValues(copyPassportValues(passportValues, entity));
        }

        return entity;
    }
}
