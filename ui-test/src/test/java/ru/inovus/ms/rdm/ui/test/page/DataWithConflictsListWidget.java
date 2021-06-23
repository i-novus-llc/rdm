package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import java.util.List;

public class DataWithConflictsListWidget extends N2oTableWidget {

    public DataFormModal fixRowForm(int rowNum) {
        columns().rows().row(rowNum).click();
        toolbar()
                .topRight()
                .button("Исправить")
                .click();
        return N2oSelenide.modal(DataFormModal.class);
    }

    public void rowShouldHaveTexts(int columnNum, List<String> text) {
        columns().rows().columnShouldHaveTexts(columnNum, text);
    }

    public void rowShouldHaveSize(int size) {
        columns().rows().shouldHaveSize(size);
    }
}
