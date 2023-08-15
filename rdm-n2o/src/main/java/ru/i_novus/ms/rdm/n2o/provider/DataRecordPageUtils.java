package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.metadata.control.N2oStandardField;
import net.n2oapp.framework.api.metadata.control.plain.N2oCheckbox;
import net.n2oapp.framework.api.metadata.control.plain.N2oDatePicker;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

public final class DataRecordPageUtils {

    private DataRecordPageUtils() {
        // Nothing to do.
    }

    public static N2oStandardField createField(FieldType type) {

        switch (type) {
            case INTEGER: return createIntegerField();
            case FLOAT: return createFloatField();
            case DATE: return createDateField();
            case BOOLEAN: return createBooleanField();
            default: return new N2oInputText();
        }
    }

    private static N2oInputText createIntegerField() {

        N2oInputText field = new N2oInputText();
        field.setDomain(N2oDomain.INTEGER);
        field.setStep("1");
        return field;
    }

    private static N2oInputText createFloatField() {

        N2oInputText field = new N2oInputText();
        field.setDomain(N2oDomain.FLOAT);
        field.setStep("0.0001");
        return field;
    }

    private static N2oDatePicker createDateField() {

        N2oDatePicker field = new N2oDatePicker();
        field.setDateFormat("DD.MM.YYYY");
        return field;
    }

    private static N2oCheckbox createBooleanField() {

        N2oCheckbox field = new N2oCheckbox();
        field.setNoLabelBlock(Boolean.TRUE);
        return field;
    }
}
