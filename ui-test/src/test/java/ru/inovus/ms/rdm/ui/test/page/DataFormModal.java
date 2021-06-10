package ru.inovus.ms.rdm.ui.test.page;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.collection.Fields;
import net.n2oapp.framework.autotest.api.component.page.SimplePage;
import net.n2oapp.framework.autotest.api.component.widget.FormWidget;
import net.n2oapp.framework.autotest.impl.component.control.N2oCheckbox;
import net.n2oapp.framework.autotest.impl.component.control.N2oDateInput;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputText;
import net.n2oapp.framework.autotest.impl.component.modal.N2oModal;

import static com.codeborne.selenide.Selectors.byClassName;
import static com.codeborne.selenide.Selenide.$;

public class DataFormModal extends N2oModal {

    public N2oInputText stringInput(String fieldName) {
        return fields().field(fieldName).control(N2oInputText.class);
    }

    public N2oInputText integerInput(String fieldName) {
        return stringInput(fieldName);
    }

    public N2oInputText doubleInput(String fieldName) {
        return stringInput(fieldName);
    }

    public N2oDateInput dateInput(String fieldName) {
        return fields().field(fieldName).control(N2oDateInput.class);
    }

    public N2oCheckbox booleanInput(String fieldName) {
        return fields().field(fieldName).control(N2oCheckbox.class);
    }

    public N2oInputSelect referenceInput(String fieldName) {
        return fields().field(fieldName).control(N2oInputSelect.class);
    }

    private Fields fields() {
        return content(SimplePage.class).widget(FormWidget.class).fields();
    }

    public void save() {
        $(byClassName("modal-footer")).$$(byClassName("btn")).find(Condition.text("Сохранить")).click();
    }

    public void edit() {
        $(byClassName("modal-footer")).$$(byClassName("btn")).find(Condition.text("Изменить")).click();
    }
}
