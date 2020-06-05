package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.version.CreateAttribute;
import ru.inovus.ms.rdm.api.model.version.UpdateAttribute;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.List;

import static java.util.Collections.singletonList;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;

@Component
public class StructureChangeValidator {

    private static final String ATTRIBUTE_CREATE_ILLEGAL_VALUE_EXCEPTION_CODE = "attribute.create.illegal.value";
    private static final String ATTRIBUTE_UPDATE_ILLEGAL_VALUE_EXCEPTION_CODE = "attribute.update.illegal.value";

    private static final String VALIDATION_REQUIRED_ERR_EXCEPTION_CODE = "validation.required.err";
    private static final String ATTRIBUTE_PRIMARY_INCOMPATIBLE_WITH_DATA_EXCEPTION_CODE = "attribute.primary.incompatible.with.data";
    private static final String ATTRIBUTE_TYPE_INCOMPATIBLE_WITH_DATA_EXCEPTION_CODE = "attribute.type.incompatible.with.data";

    private DraftDataService draftDataService;
    private SearchDataService searchDataService;
    private RefBookVersionRepository versionRepository;

    @Autowired
    public StructureChangeValidator(DraftDataService draftDataService, SearchDataService searchDataService, RefBookVersionRepository versionRepository) {
        this.draftDataService = draftDataService;
        this.searchDataService = searchDataService;
        this.versionRepository = versionRepository;
    }

    public void validateCreateAttribute(CreateAttribute createAttribute) {

        Structure.Attribute newAttribute = createAttribute.getAttribute();
        if (newAttribute == null
                || StringUtils.isEmpty(newAttribute.getCode())
                || newAttribute.getType() == null)
            throw new IllegalArgumentException(ATTRIBUTE_CREATE_ILLEGAL_VALUE_EXCEPTION_CODE);

        Structure.Reference reference = createAttribute.getReference();
        boolean hasReference = reference != null && !reference.isNull();

        if (newAttribute.isReferenceType() != hasReference)
            throw new IllegalArgumentException(ATTRIBUTE_CREATE_ILLEGAL_VALUE_EXCEPTION_CODE);

        if (hasReference && !newAttribute.getCode().equals(reference.getAttribute()))
            throw new IllegalArgumentException(ATTRIBUTE_CREATE_ILLEGAL_VALUE_EXCEPTION_CODE);
    }

    public void validateCreateAttributeStorage(Structure.Attribute newAttribute,
                                               Structure oldStructure, String storageCode) {

        if (oldStructure == null || oldStructure.getAttributes() == null || !newAttribute.hasIsPrimary())
            return;

        // Проверка наличия данных для добавляемого атрибута, обязательного к заполнению
        if (searchDataService.hasData(storageCode))
            throw new UserException(new Message(VALIDATION_REQUIRED_ERR_EXCEPTION_CODE, newAttribute.getName()));
    }

    public void validateUpdateAttribute(UpdateAttribute updateAttribute, Structure.Attribute oldAttribute) {

        if (oldAttribute == null
                || updateAttribute.getVersionId() == null
                || StringUtils.isEmpty(updateAttribute.getCode())
                || updateAttribute.getType() == null)
            throw new IllegalArgumentException(ATTRIBUTE_UPDATE_ILLEGAL_VALUE_EXCEPTION_CODE);

        if (!updateAttribute.isReferenceType())
            return;

        if ((oldAttribute.isReferenceType() && !updateAttribute.isNullOrPresentReference())
                || (!oldAttribute.isReferenceType() && !updateAttribute.isNotNullAndPresentReference())
                || !updateAttribute.getCode().equals(updateAttribute.getAttribute().get()))
            throw new IllegalArgumentException(ATTRIBUTE_UPDATE_ILLEGAL_VALUE_EXCEPTION_CODE);
    }

    public void validateUpdateAttributeStorage(UpdateAttribute updateAttribute,
                                               Structure.Attribute oldAttribute, String storageCode) {

        if (updateAttribute.hasIsPrimary()) {
            // Проверка отсутствия пустых значений в поле при установке первичного ключа
            if (draftDataService.isFieldContainEmptyValues(storageCode, updateAttribute.getCode())) {
                throw new UserException(new Message(ATTRIBUTE_PRIMARY_INCOMPATIBLE_WITH_DATA_EXCEPTION_CODE, oldAttribute.getName()));
            }

            validatePrimaryKeyUnique(storageCode, updateAttribute);
        }

        // Если столбец для поля пустой, то проверка с учётом наличия данных не нужна
        if (!draftDataService.isFieldNotEmpty(storageCode, updateAttribute.getCode()))
            return;

        if (!isCompatibleTypes(oldAttribute.getType(), updateAttribute.getType()))
            throw new UserException(new Message(ATTRIBUTE_TYPE_INCOMPATIBLE_WITH_DATA_EXCEPTION_CODE, oldAttribute.getName()));

        if (updateAttribute.isReferenceType() && !oldAttribute.isReferenceType()) {
            validateReferenceValues(updateAttribute);
        }
    }

    private void validatePrimaryKeyUnique(String storageCode, UpdateAttribute updateAttribute) {

        List<Message> errorMessages = new PrimaryKeyUniqueValidation(
                draftDataService,
                storageCode,
                singletonList(updateAttribute.getCode())
        ).validate();

        if (!CollectionUtils.isEmpty(errorMessages))
            throw new UserException(errorMessages);
    }

    private void validateReferenceValues(UpdateAttribute updateAttribute) {

        Structure.Reference reference = new Structure.Reference(
                updateAttribute.getAttribute().get(),
                updateAttribute.getReferenceCode().get(),
                updateAttribute.getDisplayExpression().get()
        );

        List<Message> errorMessages = new ReferenceValidation(
                searchDataService,
                versionRepository,
                reference,
                updateAttribute.getVersionId()
        ).validate();

        if (!CollectionUtils.isEmpty(errorMessages))
            throw new UserException(errorMessages);
    }

    private boolean isCompatibleTypes(FieldType realDataType, FieldType newDataType) {
        return realDataType.equals(newDataType)
                || STRING.equals(realDataType) || STRING.equals(newDataType);
    }
}
