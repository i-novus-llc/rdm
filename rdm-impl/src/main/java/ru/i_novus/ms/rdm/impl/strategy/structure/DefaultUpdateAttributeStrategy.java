package ru.i_novus.ms.rdm.impl.strategy.structure;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.components.common.exception.CodifiedException;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

@Component
public class DefaultUpdateAttributeStrategy implements UpdateAttributeStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private DraftDataService draftDataService;

    @Autowired
    private VersionValidation versionValidation;

    @Autowired
    private StructureChangeValidator structureChangeValidator;

    @Override
    public Structure.Attribute update(RefBookVersionEntity entity, UpdateAttributeRequest request) {

        final Structure structure = entity.getStructure();
        Structure.Attribute oldAttribute = structure.getAttribute(request.getCode());
        structureChangeValidator.validateUpdateAttribute(entity.getId(), request, oldAttribute);

        final String refBookCode = entity.getRefBook().getCode();
        Structure.Attribute newAttribute = Structure.Attribute.build(oldAttribute);
        request.fillAttribute(newAttribute);
        versionValidation.validateNewAttribute(newAttribute, structure, refBookCode);

        Structure.Reference oldReference = structure.getReference(oldAttribute.getCode());
        Structure.Reference newReference = null;
        if (newAttribute.isReferenceType()) {
            newReference = Structure.Reference.build(oldReference);
            request.fillReference(newReference);
            versionValidation.validateNewReference(newAttribute, newReference, structure, refBookCode);
        }

        structureChangeValidator.validateUpdateAttributeStorage(entity.getId(), request, oldAttribute, entity.getStorageCode());
        updateStorageField(entity, newAttribute);

        // Должен быть только один первичный ключ:
        if (newAttribute.hasIsPrimary())
            structure.clearPrimary();

        structure.update(oldAttribute, newAttribute);
        structure.update(oldReference, newReference);

        // Обновление значений ссылки только по необходимости:
        if (!StructureUtils.isDisplayExpressionEquals(oldReference, newReference)) {
            refreshReferenceDisplayValues(entity, newReference);
        }

        return newAttribute;
    }

    private void updateStorageField(RefBookVersionEntity entity, Structure.Attribute attribute) {
        try {
            draftDataService.updateField(entity.getStorageCode(), ConverterUtil.field(attribute));

        } catch (CodifiedException ce) {
            throw new UserException(new Message(ce.getMessage(), ce.getArgs()), ce);
        }
    }

    /**
     * Обновление отображаемого значения ссылки во всех записях с заполненным значением ссылки.
     *
     * @param draftEntity сущность-черновик
     * @param reference   атрибут-ссылка
     */
    private void refreshReferenceDisplayValues(RefBookVersionEntity draftEntity, Structure.Reference reference) {

        if (reference == null) return;

        RefBookVersionEntity publishedEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(reference.getReferenceCode(), RefBookVersionStatus.PUBLISHED);
        if (publishedEntity == null) return;

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(publishedEntity.getStructure());
        if (referenceAttribute == null) return;

        Reference updatedReference = new Reference(
                publishedEntity.getStorageCode(),
                publishedEntity.getFromDate(), // SYS_PUBLISH_TIME is not exist for draft
                referenceAttribute.getCode(),
                new DisplayExpression(reference.getDisplayExpression()),
                null, // Old value is not changed
                null // Display value will be recalculated
        );
        ReferenceFieldValue fieldValue = new ReferenceFieldValue(reference.getAttribute(), updatedReference);

        // RDM-884: Для обязательных атрибутов: если новое значение null, кидать ошибку required value
        draftDataService.updateReferenceInRefRows(draftEntity.getStorageCode(), fieldValue, null, null);
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftEntity.getId());
    }
}
