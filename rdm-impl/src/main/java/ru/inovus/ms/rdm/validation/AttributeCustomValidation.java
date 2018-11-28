package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.AttributeValidationEntity;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.*;
import ru.inovus.ms.rdm.validation.resolver.*;

import java.util.*;

/**
 * Пользовательские проверки значений атрибутов
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

    private Map<String, List<AttributeValidationResolver>> setResolvers(List<AttributeValidationEntity> attributeValidations) {
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
        return resolvers;
    }

    private AttributeValidationResolver toResolver(AttributeValidationEntity validationEntity) {
        Structure.Attribute attribute = structure.getAttribute(validationEntity.getAttribute());
        AttributeValidationValue validationValue =
                validationEntity.getType().getValidationInstance().valueFromString(validationEntity.getValue());
        switch (validationEntity.getType()) {
            case REQUIRED:
                return new RequiredAttributeValidationResolver(attribute);
            case UNIQUE:
                return new UniqueAttributeValidationResolver(attribute, searchDataService, storageCode);
            case PLAIN_SIZE:
                return new PlainSizeAttributeValidationResolver(attribute, (PlainSizeValidationValue) validationValue);
            case FLOAT_SIZE:
                return new FloatSizeAttributeValidationResolver(attribute, (FloatSizeValidationValue) validationValue);
            case INT_RANGE:
                return new IntRangeAttributeValidationResolver(attribute, (IntRangeValidationValue) validationValue);
            case FLOAT_RANGE:
                return new FloatRangeAttributeValidationResolver(attribute, (FloatRangeValidationValue) validationValue);
            case DATE_RANGE:
                return new DateRangeAttributeValidationResolver(attribute, (DateRangeValidationValue) validationValue);
            case REG_EXP:
                return new RegExpAttributeValidationResolver(attribute, (RegExpValidationValue) validationValue);
            default:
                throw new RdmException("resolver not found");
        }
    }

    @Override
    protected List<Message> validate(Map<String, Object> attributeValues) {
        List<Message> messages = new ArrayList<>();
        resolvers.entrySet().stream()
                .filter(e -> !getErrorAttributes().contains(e.getKey()))
                .forEach(e -> {
                    for (AttributeValidationResolver resolver : e.getValue()) {
                        Message message = resolver.resolve(attributeValues.get(e.getKey()));
                        if (message != null) {
                            messages.add(message);
                            break;
                        }
                    }
                });
        return messages;
    }


}
