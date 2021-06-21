package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedCreateAttributeStrategy implements CreateAttributeStrategy {

    @Autowired
    @Qualifier("defaultCreateAttributeStrategy")
    private CreateAttributeStrategy createAttributeStrategy;

    @Autowired
    private UnversionedChangeStructureStrategy unversionedChangeStructureStrategy;

    @Override
    public Structure.Attribute create(RefBookVersionEntity entity, CreateAttributeRequest request) {

        Structure.Attribute attribute = createAttributeStrategy.create(entity, request);

        unversionedChangeStructureStrategy.processReferrers(entity);

        return attribute;
    }
}
