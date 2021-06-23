package ru.inovus.ms.rdm.ui.test.page;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.collection.Fields;
import net.n2oapp.framework.autotest.api.component.page.SimplePage;
import net.n2oapp.framework.autotest.api.component.widget.FormWidget;
import net.n2oapp.framework.autotest.impl.component.control.*;
import net.n2oapp.framework.autotest.impl.component.modal.N2oModal;

import static com.codeborne.selenide.Selectors.byClassName;
import static com.codeborne.selenide.Selenide.$;

public class StructureFormModal extends N2oModal {

    public N2oInputText codeInput() {
        return fields().field("Код").control(N2oInputText.class);
    }

    public N2oInputText nameInput() {
        return fields().field("Наименование").control(N2oInputText.class);
    }

    public N2oSelect typeInput() {
        return fields().field("Тип").control(N2oSelect.class);
    }

    public N2oInputSelect refBookInput() {
        return fields().field("Выбор справочника").control(N2oInputSelect.class);
    }

    public N2oInputSelect displayAttrInput() {
        return fields().field("Отображаемый атрибут").control(N2oInputSelect.class);
    }

    public N2oCheckbox primaryKeyInput() {
        return fields().field("Первичный ключ").control(N2oCheckbox.class);
    }

    public void save() {
        $(byClassName("modal-footer")).$$(byClassName("btn")).find(Condition.text("Сохранить")).click();
    }

    private Fields fields() {
        return content(SimplePage.class).widget(FormWidget.class).fields();
    }

}
