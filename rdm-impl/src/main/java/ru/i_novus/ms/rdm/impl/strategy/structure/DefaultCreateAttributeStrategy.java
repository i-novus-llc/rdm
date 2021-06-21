package ru.i_novus.ms.rdm.impl.strategy.structure;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.components.common.exception.CodifiedException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

@Component
public class DefaultCreateAttributeStrategy implements CreateAttributeStrategy {

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private DraftDataService draftDataService;

    @Autowired
    private VersionValidation versionValidation;

    @Autowired
    private StructureChangeValidator structureChangeValidator;

    @Override
    public Structure.Attribute create(RefBookVersionEntity entity, CreateAttributeRequest request) {

        final Structure structure = entity.getStructure();
        structureChangeValidator.validateCreateAttribute(request, structure);

        final String refBookCode = entity.getRefBook().getCode();
        Structure.Attribute attribute = request.getAttribute();
        versionValidation.validateNewAttribute(attribute, structure, refBookCode);

        Structure.Reference reference = request.getReference();
        if (reference != null && reference.isNull()) {
            reference = null;
        }
        if (reference != null) {
            versionValidation.validateNewReference(attribute, reference, structure, refBookCode);
        }

        structureChangeValidator.validateCreateAttributeStorage(attribute, structure, entity.getStorageCode());
        createStorageField(entity, attribute);

        if (attribute.hasIsPrimary()) {
            // На данный момент первичным ключом может быть только одно поле.
            structure.clearPrimary();
        }

        structure.add(attribute, reference);
        entity.setStructure(structure);

        conflictRepository.deleteByReferrerVersionIdAndRefFieldCodeAndRefRecordIdIsNull(entity.getId(), attribute.getCode());

        return attribute;
    }

    private void createStorageField(RefBookVersionEntity entity, Structure.Attribute attribute) {
        try {
            draftDataService.addField(entity.getStorageCode(), ConverterUtil.field(attribute));

        } catch (CodifiedException ce) {
            throw new UserException(new Message(ce.getMessage(), ce.getArgs()), ce);
        }
    }
}
