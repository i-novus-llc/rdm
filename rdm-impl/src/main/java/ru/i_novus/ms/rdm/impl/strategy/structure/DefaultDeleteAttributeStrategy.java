package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.util.ErrorUtil;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

@Component
public class DefaultDeleteAttributeStrategy implements DeleteAttributeStrategy {

    @Autowired
    private DraftDataService draftDataService;

    @Autowired
    private VersionValidation versionValidation;

    @Override
    public Structure.Attribute delete(RefBookVersionEntity entity, DeleteAttributeRequest request) {

        final Structure structure = entity.getStructure();
        final String attributeCode = request.getAttributeCode();
        final String refBookCode = entity.getRefBook().getCode();

        Structure.Attribute attribute = structure.getAttribute(attributeCode);
        versionValidation.validateOldAttribute(attribute, structure, refBookCode);

        deleteStorageField(entity, attributeCode);

        structure.remove(attributeCode);

        return attribute;
    }

    private void deleteStorageField(RefBookVersionEntity entity, String attributeCode) {
        try {
            draftDataService.deleteField(entity.getStorageCode(), attributeCode);

        } catch (RuntimeException e) {
            ErrorUtil.rethrowError(e);
        }
    }
}
