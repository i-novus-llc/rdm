package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;

/**
 * Таблица на вкладке "Данные с конфликтами" версии справочника.
 */
public class DataTableWithConflictsWidget extends RefBookEditTableWidget {

    public DataRowForm fixRowForm(int rowNum) {

        selectRow(rowNum);
        clickButton(toolbar().topRight(), "Исправить");

        return N2oSelenide.modal(DataRowForm.class);
    }
}
