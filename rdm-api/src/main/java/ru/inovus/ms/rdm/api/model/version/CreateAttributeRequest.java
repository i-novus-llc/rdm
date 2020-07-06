package ru.inovus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModel;
import ru.inovus.ms.rdm.api.model.Structure;

@ApiModel(value = "Модель создания атрибута черновика",
        description = "Набор входных параметров для создания атрибута черновика")
public class CreateAttributeRequest extends RefBookVersionAttribute {

    @SuppressWarnings("unused")
    public CreateAttributeRequest() {
        // Nothing to do.
    }

    public CreateAttributeRequest(Integer versionId,
                                  Integer optLockValue,
                                  Structure.Attribute attribute,
                                  Structure.Reference reference) {
        super(versionId, optLockValue, attribute, reference);
    }
}