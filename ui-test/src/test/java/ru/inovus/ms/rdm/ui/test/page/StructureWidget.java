package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import java.util.List;

/**
 * Таблица на вкладке "Структура" версии справочника.
 */
public class StructureWidget extends N2oTableWidget {

    public AttributeForm openAddForm() {

        toolbar().bottomRight().button("Добавить").click();

        return N2oSelenide.modal(AttributeForm.class);
    }

    public void rowShouldHaveTexts(int columnNum, List<String> text) {
        columns().rows().columnShouldHaveTexts(columnNum, text);
    }
}
