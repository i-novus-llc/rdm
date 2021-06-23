package ru.inovus.ms.rdm.api.model.validation;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;

import java.util.Objects;

import static java.lang.Integer.parseInt;

public class FloatSizeAttributeValidation extends AttributeValidation {

    private int intPartSize;
    private int fracPartSize;


    public FloatSizeAttributeValidation() {
        super(AttributeValidationType.FLOAT_SIZE);
    }

    public FloatSizeAttributeValidation(int intPartSize, int fracPartSize) {
        this();
        this.intPartSize = intPartSize;
        this.fracPartSize = fracPartSize;
    }

    public int getIntPartSize() {
        return intPartSize;
    }

    public void setIntPartSize(int intPartSize) {
        this.intPartSize = intPartSize;
    }

    public int getFracPartSize() {
        return fracPartSize;
    }

    public void setFracPartSize(int fracPartSize) {
        this.fracPartSize = fracPartSize;
    }

    @Override
    public String valuesToString() {
        return intPartSize + ";" + fracPartSize;
    }

    @Override
    public FloatSizeAttributeValidation valueFromString(String value) {
        if (value == null || !value.matches("(\\d+)?;(\\d+)?"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (!StringUtils.isEmpty(split[0]))
            intPartSize = parseInt(split[0]);
        if (!StringUtils.isEmpty(split[1]))
            fracPartSize = parseInt(split[1]);
        return this;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FloatSizeAttributeValidation that = (FloatSizeAttributeValidation) o;
        return intPartSize == that.intPartSize &&
                fracPartSize == that.fracPartSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), intPartSize, fracPartSize);
    }
}
