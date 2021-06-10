package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import java.util.List;

public class StructureListWidget extends N2oTableWidget {

    public StructureFormModal form() {
        toolbar()
                .bottomRight()
                .button("Добавить")
                .click();
        return N2oSelenide.modal(StructureFormModal.class);
    }

    public void rowShouldHaveTexts(int columnNum, List<String> text) {
        columns().rows().columnShouldHaveTexts(columnNum, text);
    }

}
