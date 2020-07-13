package ru.i_novus.ms.rdm.api.model.field;

import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.ms.rdm.api.model.Structure;

/**
 * Ссылочное значение.
 *
 * Используется для формирования промежуточных данных с целью дальнейшей обработки.
 */
public class ReferenceFilterValue {

    // Атрибут, на который ссылаются.
    private Structure.Attribute attribute;

    // Значение ссылающегося поля.
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
