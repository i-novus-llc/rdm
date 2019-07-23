package ru.inovus.ms.rdm.model.field;

import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.inovus.ms.rdm.model.Structure;

public class ReferenceFilterValue {

    // Атрибут.
    private Structure.Attribute attribute;

    // Значение поля.
    private ReferenceFieldValue referenceValue;

    public ReferenceFilterValue(Structure.Attribute attribute, ReferenceFieldValue referenceValue) {
        this.attribute = attribute;
        this.referenceValue = referenceValue;
    }

    public Structure.Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Structure.Attribute attribute) {
        this.attribute = attribute;
    }

    public ReferenceFieldValue getReferenceValue() {
        return referenceValue;
    }

    public void setReferenceValue(ReferenceFieldValue referenceValue) {
        this.referenceValue = referenceValue;
    }
}
