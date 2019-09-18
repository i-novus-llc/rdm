package ru.inovus.ms.rdm.n2o.model.version;

import ru.inovus.ms.rdm.n2o.model.Structure;

public class CreateAttribute extends RefBookVersionAttribute {

    @SuppressWarnings("unused")
    public CreateAttribute() {}

    public CreateAttribute(Integer versionId, Structure.Attribute attribute, Structure.Reference reference) {
        super(versionId, attribute, reference);
    }
}