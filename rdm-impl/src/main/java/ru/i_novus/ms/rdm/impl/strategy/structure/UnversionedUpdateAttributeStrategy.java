package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedUpdateAttributeStrategy implements UpdateAttributeStrategy {

    @Autowired
    @Qualifier("defaultUpdateAttributeStrategy")
    private UpdateAttributeStrategy updateAttributeStrategy;

    @Autowired
    private UnversionedChangeStructureStrategy unversionedChangeStructureStrategy;

    @Override
    public Structure.Attribute update(RefBookVersionEntity entity, UpdateAttributeRequest request) {

        Structure.Attribute attribute = updateAttributeStrategy.update(entity, request);

        unversionedChangeStructureStrategy.processReferrers(entity);

        return attribute;
    }
}
