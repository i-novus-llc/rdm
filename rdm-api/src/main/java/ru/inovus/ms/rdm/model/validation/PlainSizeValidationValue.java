package ru.inovus.ms.rdm.model.validation;

import io.swagger.annotations.ApiModel;
import net.n2oapp.platform.i18n.UserException;

@ApiModel(parent = AttributeValidationValue.class)
public class PlainSizeValidationValue extends AttributeValidationValue {

    private int size;

    public PlainSizeValidationValue() {
        super(AttributeValidationType.PLAIN_SIZE);
    }

    public PlainSizeValidationValue(int size) {
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
    public PlainSizeValidationValue valueFromString(String value) {
        if (value == null || !value.matches("^\\d$"))
            throw new UserException("attribute.validation.value.invalid");
        size = Integer.parseInt(value);
        return this;
    }
}
