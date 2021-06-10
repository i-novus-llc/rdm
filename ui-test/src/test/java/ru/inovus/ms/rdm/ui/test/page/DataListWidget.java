package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.impl.component.page.N2oSimplePage;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import java.util.List;

import static net.n2oapp.framework.autotest.N2oSelenide.page;

public class DataListWidget extends N2oTableWidget {

    public DataFormModal addRowForm() {
        toolbar()
                .topRight()
                .button("Добавить")
                .click();
        return N2oSelenide.modal(DataFormModal.class);
    }

    public DataFormModal editRowForm(int rowNum) {
        columns().rows().row(rowNum).click();
        toolbar()
                .topRight()
                .button("Изменить")
                .click();
        return N2oSelenide.modal(DataFormModal.class);
    }

    public void deleteRowForm(int rowNum) {
        columns().rows().row(rowNum).click();
        toolbar()
                .topRight()
                .button("Удалить")
                .click();
        Page.Dialog deleteDialog = page(N2oSimplePage.class).dialog("Удалить");
        deleteDialog.shouldBeVisible();
        deleteDialog.click("Да");
    }

    public void rowShouldHaveTexts(int columnNum, List<String> text) {
        columns().rows().columnShouldHaveTexts(columnNum, text);
    }

    public void rowShouldHaveSize(int size) {
        columns().rows().shouldHaveSize(size);
    }
}
