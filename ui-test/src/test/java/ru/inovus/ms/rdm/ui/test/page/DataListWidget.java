package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.impl.component.page.N2oSimplePage;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import java.util.List;

import static net.n2oapp.framework.autotest.N2oSelenide.page;

/**
 * Таблица на вкладке "Данные" версии справочника.
 */
public class DataListWidget extends N2oTableWidget {

    public DataRowForm openAddRowForm() {

        toolbar().topRight().button("Добавить").click();

        return N2oSelenide.modal(DataRowForm.class);
    }

    public DataRowForm openEditRowForm(int rowNum) {

        columns().rows().row(rowNum).click();
        toolbar().topRight().button("Изменить").click();

        return N2oSelenide.modal(DataRowForm.class);
    }

    public void deleteRow(int rowNum) {

        columns().rows().row(rowNum).click();
        toolbar().topRight().button("Удалить").click();

        Page.Dialog deleteDialog = page(N2oSimplePage.class).dialog("Удалить");
        deleteDialog.shouldBeVisible();
        deleteDialog.button("Да").click();
    }

    public void rowShouldHaveTexts(int columnNum, List<String> text) {
        columns().rows().columnShouldHaveTexts(columnNum, text);
    }

    public void rowShouldHaveSize(int size) {
        columns().rows().shouldHaveSize(size);
    }
}
