package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.version.UpdateAttribute;
import ru.inovus.ms.rdm.api.model.version.UpdateValue;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;

@Component
public class AttributeUpdateValidator {

    private static final String ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE = "Can not update structure, illegal update attribute";
    private static final String INCOMPATIBLE_NEW_STRUCTURE_EXCEPTION_CODE = "incompatible.new.structure";
    private static final String INCOMPATIBLE_NEW_TYPE_EXCEPTION_CODE = "incompatible.new.type";

    private DraftDataService draftDataService;
    private SearchDataService searchDataService;
    private RefBookVersionRepository versionRepository;

    @Autowired
    public AttributeUpdateValidator(DraftDataService draftDataService, SearchDataService searchDataService, RefBookVersionRepository versionRepository) {
        this.draftDataService = draftDataService;
        this.searchDataService = searchDataService;
        this.versionRepository = versionRepository;
    }

    @SuppressWarnings({"squid:S1067", "squid:S3776"})
    public void validateUpdateAttribute(UpdateAttribute updateAttribute, Structure.Attribute attribute) {

        if (attribute == null
                || updateAttribute.getVersionId() == null
                || updateAttribute.getType() == null)
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);

        if (updateAttribute.isReferenceType() &&
                ((attribute.isReferenceType() && isValidUpdateReferenceValues(updateAttribute, this::isUpdateValueNotNullAndEmpty))
                        || (!attribute.isReferenceType() && isValidUpdateReferenceValues(updateAttribute, this::isUpdateValueNullOrEmpty))
                ))
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);
    }

    @SuppressWarnings({"squid:S1067", "squid:S3776"})
    public void validateUpdateAttributeStorage(UpdateAttribute updateAttribute, Structure.Attribute attribute, String storageCode) {

        if (updateAttribute.hasIsPrimary()) {
            // Проверка отсутствия пустых значений в поле при установке первичного ключа
            if (draftDataService.isFieldContainEmptyValues(storageCode, updateAttribute.getCode())) {
                throw new UserException(new Message(INCOMPATIBLE_NEW_STRUCTURE_EXCEPTION_CODE, attribute.getName()));
            }

            validatePrimaryKeyUnique(storageCode, updateAttribute);
        }

        // проверка совместимости типов, если столбец не пустой и изменяется тип. Если пустой - можно изменить тип
        if (draftDataService.isFieldNotEmpty(storageCode, updateAttribute.getCode())) {
            if (!isCompatibleTypes(attribute.getType(), updateAttribute.getType())) {
                throw new UserException(new Message(INCOMPATIBLE_NEW_TYPE_EXCEPTION_CODE, attribute.getName()));
            }
        } else
            return;

        if (updateAttribute.isReferenceType() && !attribute.isReferenceType()) {
            validateReferenceValues(updateAttribute);
        }
    }

    private void validatePrimaryKeyUnique(String storageCode, UpdateAttribute updateAttribute) {
        List<Message> pkValidationMessages = new PrimaryKeyUniqueValidation(draftDataService, storageCode,
                singletonList(updateAttribute.getCode())).validate();
        if (pkValidationMessages != null && !pkValidationMessages.isEmpty())
            throw new UserException(pkValidationMessages);
    }

    private void validateReferenceValues(UpdateAttribute updateAttribute) {
        List<Message> referenceValidationMessages = new ReferenceValidation(
                searchDataService,
                versionRepository,
                new Structure.Reference(updateAttribute.getAttribute().get(), updateAttribute.getReferenceCode().get(), updateAttribute.getDisplayExpression().get()),
                updateAttribute.getVersionId()).validate();
        if (!isEmpty(referenceValidationMessages))
            throw new UserException(referenceValidationMessages);
    }

    private boolean isCompatibleTypes(FieldType realDataType, FieldType newDataType) {
        return realDataType.equals(newDataType) || STRING.equals(realDataType) || STRING.equals(newDataType);
    }

    private boolean isValidUpdateReferenceValues(UpdateAttribute updateAttribute, Predicate<UpdateValue> valueValidateFunc) {
        return valueValidateFunc.test(updateAttribute.getReferenceCode())
                || valueValidateFunc.test(updateAttribute.getAttribute());
    }

    private boolean isUpdateValueNotNullAndEmpty(UpdateValue updateValue) {
        return updateValue != null && !updateValue.isPresent();
    }

    private boolean isUpdateValueNullOrEmpty(UpdateValue updateValue) {
        return updateValue == null || !updateValue.isPresent();
    }

}
