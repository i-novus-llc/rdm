package ru.inovus.ms.rdm.api.model.validation;

import net.n2oapp.platform.i18n.UserException;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExpAttributeValidation extends AttributeValidation {

    private String regExp;

    public RegExpAttributeValidation() {
        super(AttributeValidationType.REG_EXP);
    }

    public RegExpAttributeValidation(String regExp) {
        this();
        this.regExp = regExp;
    }

    public String getRegExp() {
        return regExp;
    }

    public void setRegExp(String regExp) {
        this.regExp = regExp;
    }

    @Override
    public String valuesToString() {
        return regExp;
    }

    @Override
    public RegExpAttributeValidation valueFromString(String value) {
        try {
            Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            throw new UserException("attribute.validation.reg.exp.invalid", e);
        }
        regExp = value;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RegExpAttributeValidation that = (RegExpAttributeValidation) o;
        return Objects.equals(regExp, that.regExp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), regExp);
    }
}
