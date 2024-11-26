package ru.inovus.ms.rdm.ui.test.util;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.component.DropDown;
import net.n2oapp.framework.autotest.api.component.control.Control;
import net.n2oapp.framework.autotest.api.component.control.InputText;
import net.n2oapp.framework.autotest.api.component.control.TextArea;
import net.n2oapp.framework.autotest.impl.component.control.N2oCheckbox;
import net.n2oapp.framework.autotest.impl.component.control.N2oDateInput;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oSelect;

public final class UiTestUtil {

    private UiTestUtil() {
        // Nothing to do.
    }

    public static void fillInputControl(Control control, String value) {

        control.shouldExists();
        fillControlValue(control, value);
        control.shouldHaveValue(value);
    }

    public static void fillControlValue(Control control, String value) {

        if (control instanceof InputText inputText) {
            inputText.setValue(value);

        } else if (control instanceof N2oSelect n2oSelect) {
            fillN2oSelectValue(n2oSelect, value);

        } else if (control instanceof N2oInputSelect n2oInputSelect) {
            fillN2oInputSelectValue(n2oInputSelect, value);

        } else if (control instanceof TextArea textArea) {
            textArea.setValue(value);

        } else if (control instanceof N2oDateInput n2oDateInput) {
            n2oDateInput.setValue(value);

        } else {
            throw new IllegalArgumentException("Control is not for input");
        }
    }

    public static void fillN2oSelectValue(N2oSelect control, String value) {

        control.openPopup();
        final DropDown dropDown = control.dropdown();
        dropDown.selectItemBy(Condition.text(value));
        control.closePopup();

        control.shouldHaveValue(value);
    }

    public static void fillN2oInputSelectValue(N2oInputSelect control, String value) {

        control.openPopup();
        final DropDown dropDown = control.dropdown();
        dropDown.selectItemBy(Condition.text(value));
        control.closePopup();

        control.shouldHaveValue(value);
    }

    public static void fillCheckBox(Control checkBox, boolean value) {

        checkBox.shouldExists();

        if (!(checkBox instanceof N2oCheckbox control))
            throw new IllegalArgumentException("Control is not check box");

        control.setChecked(value);

        if (value) {
            control.shouldBeChecked();
        } else {
            control.shouldBeEmpty();
        }
    }
}
