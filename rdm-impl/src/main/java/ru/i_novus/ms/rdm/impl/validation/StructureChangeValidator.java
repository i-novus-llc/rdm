package ru.i_novus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;

@Component
public class StructureChangeValidator {

    private static final String ATTRIBUTE_CREATE_ILLEGAL_VALUE_EXCEPTION_CODE = "attribute.create.illegal.value";
    private static final String ATTRIBUTE_CREATE_ILLEGAL_REFERENCE_VALUE_EXCEPTION_CODE = "attribute.create.illegal.reference.value";
    private static final String ATTRIBUTE_UPDATE_ILLEGAL_VALUE_EXCEPTION_CODE = "attribute.update.illegal.value";
    private static final String ATTRIBUTE_UPDATE_ILLEGAL_REFERENCE_VALUE_EXCEPTION_CODE = "attribute.update.illegal.reference.value";

    private static final String ATTRIBUTE_WITH_CODE_ALREADY_EXISTS_EXCEPTION_CODE = "attribute.with.code.already.exists";
    private static final String VALIDATION_REQUIRED_PK_ERR_EXCEPTION_CODE = "validation.required.pk.err";
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

    public void validateCreateAttribute(CreateAttributeRequest request, Structure oldStructure) {

        Structure.Attribute newAttribute = request.getAttribute();
        if (newAttribute == null
                || StringUtils.isEmpty(newAttribute.getCode())
                || newAttribute.getType() == null)
            throw new IllegalArgumentException(ATTRIBUTE_CREATE_ILLEGAL_VALUE_EXCEPTION_CODE);

        final String attributeCode = newAttribute.getCode();
        if (oldStructure != null && oldStructure.getAttribute(attributeCode) != null)
            throw new UserException(new Message(ATTRIBUTE_WITH_CODE_ALREADY_EXISTS_EXCEPTION_CODE, attributeCode));

        Structure.Reference reference = request.getReference();
        boolean isReference = StructureUtils.isReference(reference);

        if (newAttribute.isReferenceType() != isReference)
            throw new IllegalArgumentException(ATTRIBUTE_CREATE_ILLEGAL_VALUE_EXCEPTION_CODE);

        if (isReference && !attributeCode.equals(reference.getAttribute()))
            throw new IllegalArgumentException(ATTRIBUTE_CREATE_ILLEGAL_REFERENCE_VALUE_EXCEPTION_CODE);
    }

    public void validateCreateAttributeStorage(Structure.Attribute newAttribute,
                                               Structure oldStructure, String storageCode) {

        if (oldStructure == null || isEmpty(oldStructure.getAttributes()) || !newAttribute.hasIsPrimary())
            return;

        // Проверка наличия данных для добавляемого первичного ключа, обязательного к заполнению
        if (searchDataService.hasData(storageCode))
            throw new UserException(new Message(VALIDATION_REQUIRED_PK_ERR_EXCEPTION_CODE, newAttribute.getName()));
    }

    public void validateUpdateAttribute(Integer draftId, UpdateAttributeRequest request, Structure.Attribute oldAttribute) {

        if (oldAttribute == null
                || draftId == null
                || StringUtils.isEmpty(request.getCode())
                || request.getType() == null)
            throw new IllegalArgumentException(ATTRIBUTE_UPDATE_ILLEGAL_VALUE_EXCEPTION_CODE);

        if (!request.isReferenceType())
            return;

        if ((oldAttribute.isReferenceType() && !request.isReferenceUpdating())
                || (!oldAttribute.isReferenceType() && !request.isReferenceFilling())
                || !request.getCode().equals(request.getAttribute().get()))
            throw new IllegalArgumentException(ATTRIBUTE_UPDATE_ILLEGAL_REFERENCE_VALUE_EXCEPTION_CODE);
    }

    public void validateUpdateAttributeStorage(Integer draftId, UpdateAttributeRequest request,
                                               Structure.Attribute oldAttribute, String storageCode) {

        if (request.hasIsPrimary()) {
            // Проверка отсутствия пустых значений в поле при установке первичного ключа
            if (draftDataService.isFieldContainEmptyValues(storageCode, request.getCode())) {
                throw new UserException(new Message(ATTRIBUTE_PRIMARY_INCOMPATIBLE_WITH_DATA_EXCEPTION_CODE, oldAttribute.getName()));
            }

            validatePrimaryKeyUnique(storageCode, request);
        }

        // Если столбец для поля пустой, то проверка с учётом наличия данных не нужна
        if (!draftDataService.isFieldNotEmpty(storageCode, request.getCode()))
            return;

        if (!isCompatibleTypes(oldAttribute.getType(), request.getType()))
            throw new UserException(new Message(ATTRIBUTE_TYPE_INCOMPATIBLE_WITH_DATA_EXCEPTION_CODE, oldAttribute.getName()));

        if (request.isReferenceType() && !oldAttribute.isReferenceType()) {
            validateReferenceValues(draftId, request);
        }
    }

    private void validatePrimaryKeyUnique(String storageCode, UpdateAttributeRequest request) {

        List<Message> errorMessages = new PrimaryKeyUniqueValidation(
                draftDataService,
                storageCode,
                singletonList(request.getCode())
        ).validate();

        if (!CollectionUtils.isEmpty(errorMessages))
            throw new UserException(errorMessages);
    }

    private void validateReferenceValues(Integer draftId, UpdateAttributeRequest request) {

        Structure.Reference reference = new Structure.Reference(
                request.getAttribute().get(),
                request.getReferenceCode().get(),
                request.getDisplayExpression().get()
        );

        List<Message> errorMessages = new ReferenceValidation(
                searchDataService, versionRepository, reference, draftId
        ).validate();

        if (!CollectionUtils.isEmpty(errorMessages))
            throw new UserException(errorMessages);
    }

    private boolean isCompatibleTypes(FieldType realDataType, FieldType newDataType) {

        return realDataType.equals(newDataType)
                || STRING.equals(realDataType) || STRING.equals(newDataType);
    }
}
