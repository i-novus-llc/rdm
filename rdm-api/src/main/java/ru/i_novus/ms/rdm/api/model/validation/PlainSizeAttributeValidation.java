package ru.i_novus.ms.rdm.api.model.validation;

import io.swagger.annotations.ApiModel;
import net.n2oapp.platform.i18n.UserException;

import java.util.Objects;

@ApiModel(parent = AttributeValidation.class)
public class PlainSizeAttributeValidation extends AttributeValidation {

    private int size;

    public PlainSizeAttributeValidation() {
        super(AttributeValidationType.PLAIN_SIZE);
    }

    public PlainSizeAttributeValidation(int size) {
        this();
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }


    /**
     * @return Максимальная длина в виде числа
     */
    @Override
    public String valuesToString() {
        return String.valueOf(size);
    }

    /**
     * Заполнение проверки из строки
     *
     * @param value целое положительное число в виде String
     * @throws IllegalArgumentException если формат не соответствует
     */
    @Override
    public PlainSizeAttributeValidation valueFromString(String value) {
        if (value == null || !value.matches("\\d+"))
            throw new UserException("attribute.validation.value.invalid");
        size = Integer.parseInt(value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlainSizeAttributeValidation that = (PlainSizeAttributeValidation) o;
        return size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), size);
    }
}
