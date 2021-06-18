package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputText;
import net.n2oapp.framework.autotest.impl.component.control.N2oTextArea;
import net.n2oapp.framework.autotest.impl.component.widget.N2oFormWidget;

public class RefBookCreateFormWidget extends N2oFormWidget {

    private RefBookListPage openedFromPage;

    public N2oInputText codeInput() {
        return fields().field("Код").control(N2oInputText.class);
    }

    public N2oInputText nameInput() {
        return fields().field("Наименование").control(N2oInputText.class);
    }

    public N2oInputText shortNameInput() {
        return fields().field("Краткое наименование").control(N2oInputText.class);
    }

    public N2oTextArea descriptionInput() {
        return fields().field("Описание").control(N2oTextArea.class);
    }

    public N2oInputSelect typeInput() {
        return fields().field("Тип").control(N2oInputSelect.class);
    }


    public RefBookEditPage save() {
        openedFromPage.toolbar().bottomRight().button("Сохранить").click();
        return N2oSelenide.page(RefBookEditPage.class);
    }

    public void setOpenedFromPage(RefBookListPage openedFromPage) {
        this.openedFromPage = openedFromPage;
    }
}
