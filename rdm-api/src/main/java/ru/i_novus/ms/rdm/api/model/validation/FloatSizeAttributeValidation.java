package ru.i_novus.ms.rdm.api.model.validation;

import net.n2oapp.platform.i18n.UserException;

import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static ru.i_novus.ms.rdm.api.util.StringUtils.isEmpty;

public class FloatSizeAttributeValidation extends AttributeValidation {

    private static final String VALUE_DELIMITER = ";";
    private static final String VALUE_RANGE_REGEX = "(\\d+)?" + VALUE_DELIMITER + "(\\d+)?";
    private static final Pattern VALUE_RANGE_PATTERN = Pattern.compile(VALUE_RANGE_REGEX);

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

        if (value == null || !VALUE_RANGE_PATTERN.matcher(value).matches())
            throw new UserException("attribute.validation.value.invalid");

        String[] split = value.split(";");
        if (!isEmpty(split[0]))
            intPartSize = parseInt(split[0]);
        if (!isEmpty(split[1]))
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
