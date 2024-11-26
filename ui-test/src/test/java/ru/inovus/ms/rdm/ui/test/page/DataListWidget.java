package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.impl.component.page.N2oSimplePage;

/**
 * Таблица на вкладке "Данные" версии справочника.
 */
public class DataListWidget extends RefBookEditTableWidget {

    public DataRowForm openAddRowForm() {

        clickButton(toolbar().topRight(), "Добавить");

        return N2oSelenide.modal(DataRowForm.class);
    }

    public DataRowForm openEditRowForm(int rowNum) {

        selectRow(rowNum);
        clickButton(toolbar().topRight(), "Изменить");

        return N2oSelenide.modal(DataRowForm.class);
    }

    public void deleteRow(int rowNum) {

        selectRow(rowNum);
        clickButton(toolbar().topRight(), "Удалить");

        final Page.Dialog deleteDialog = N2oSelenide.page(N2oSimplePage.class).dialog("Удалить");
        deleteDialog.shouldBeVisible();
        deleteDialog.button("Да").click();
    }
}
