package ru.inovus.ms.rdm.model.validation;

import net.n2oapp.platform.i18n.UserException;

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
            throw new UserException("attribute.validation.value.invalid",e);
        }
        regExp = value;
        return this;
    }
}
