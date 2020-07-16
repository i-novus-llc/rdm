package ru.i_novus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.validation.*;
import ru.i_novus.ms.rdm.api.model.version.UniqueAttributeValue;
import ru.i_novus.ms.rdm.impl.entity.AttributeValidationEntity;
import ru.i_novus.ms.rdm.impl.validation.resolver.*;

import java.util.*;

/**
 * Пользовательские проверки значений атрибутов.
 */
public class AttributeCustomValidation extends AppendRowValidation {

    private final Structure structure;
    private final SearchDataService searchDataService;
    private final String storageCode;

    private Map<String, List<AttributeValidationResolver>> resolvers;

    public AttributeCustomValidation(List<AttributeValidationEntity> attributeValidations, Structure structure,
                                     SearchDataService searchDataService, String storageCode) {
        this.structure = structure;
        this.searchDataService = searchDataService;
        this.storageCode = storageCode;

        setResolvers(attributeValidations);
    }

    private void setResolvers(List<AttributeValidationEntity> attributeValidations) {

        attributeValidations.sort(Comparator.comparing(AttributeValidationEntity::getType));

        resolvers = new HashMap<>();
        for (AttributeValidationEntity attributeValidation : attributeValidations) {
            AttributeValidationResolver resolver = toResolver(attributeValidation);
            if (resolvers.containsKey(attributeValidation.getAttribute())) {
                resolvers.get(attributeValidation.getAttribute()).add(resolver);
            } else {
                ArrayList<AttributeValidationResolver> newResolverList = new ArrayList<>();
                newResolverList.add(resolver);
                resolvers.put(attributeValidation.getAttribute(), newResolverList);
            }
        }
    }

    private AttributeValidationResolver toResolver(AttributeValidationEntity validationEntity) {

        Structure.Attribute attribute = structure.getAttribute(validationEntity.getAttribute());
        AttributeValidation validationValue =
                validationEntity.getType().getValidationInstance().valueFromString(validationEntity.getValue());

        switch (validationEntity.getType()) {
            case REQUIRED:
                return new RequiredAttributeValidationResolver(attribute);
            case UNIQUE:
                return new UniqueAttributeValidationResolver(attribute, searchDataService, storageCode);
            case PLAIN_SIZE:
                return new PlainSizeAttributeValidationResolver(attribute, (PlainSizeAttributeValidation) validationValue);
            case FLOAT_SIZE:
                return new FloatSizeAttributeValidationResolver(attribute, (FloatSizeAttributeValidation) validationValue);
            case INT_RANGE:
                return new IntRangeAttributeValidationResolver(attribute, (IntRangeAttributeValidation) validationValue);
            case FLOAT_RANGE:
                return new FloatRangeAttributeValidationResolver(attribute, (FloatRangeAttributeValidation) validationValue);
            case DATE_RANGE:
                return new DateRangeAttributeValidationResolver(attribute, (DateRangeAttributeValidation) validationValue);
            case REG_EXP:
                return new RegExpAttributeValidationResolver(attribute, (RegExpAttributeValidation) validationValue);
            default:
                throw new RdmException("resolver not found");
        }
    }

    @Override
    protected List<Message> validate(Long systemId, Map<String, Object> rowData) {

        List<Message> messages = new ArrayList<>();
        resolvers.entrySet().stream()
                .filter(e -> !getErrorAttributes().contains(e.getKey()))
                .forEach(e -> {
                    for (AttributeValidationResolver resolver : e.getValue()) {
                        Object attributeValue =
                                resolver instanceof UniqueAttributeValidationResolver
                                        ? new UniqueAttributeValue(systemId, rowData.get(e.getKey()))
                                        : rowData.get(e.getKey());
                        @SuppressWarnings("unchecked")
                        Message message = resolver.resolve(attributeValue);
                        if (message != null) {
                            messages.add(message);
                            break;
                        }
                    }
                });
        return messages;
    }
}
