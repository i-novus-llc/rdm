package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedDeleteAttributeStrategy implements DeleteAttributeStrategy {

    @Autowired
    @Qualifier("defaultDeleteAttributeStrategy")
    private DeleteAttributeStrategy deleteAttributeStrategy;

    @Autowired
    private UnversionedChangeStructureStrategy unversionedChangeStructureStrategy;

    @Override
    public Structure.Attribute delete(RefBookVersionEntity entity, DeleteAttributeRequest request) {

        boolean hasReferrers = unversionedChangeStructureStrategy.hasReferrerVersions(entity);
        Structure oldStructure = hasReferrers ? new Structure(entity.getStructure()) : null;

        Structure.Attribute attribute = deleteAttributeStrategy.delete(entity, request);

        if (hasReferrers) {
            unversionedChangeStructureStrategy.validatePrimariesEquality(
                    entity.getRefBook().getCode(), oldStructure, entity.getStructure()
            );
            unversionedChangeStructureStrategy.processReferrers(entity);
        }

        return attribute;
    }
}
