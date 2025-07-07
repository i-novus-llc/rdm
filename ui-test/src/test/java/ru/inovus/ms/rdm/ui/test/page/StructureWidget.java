package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;

/**
 * Таблица на вкладке "Структура" версии справочника.
 */
public class StructureWidget extends RefBookEditTableWidget {

    public AttributeForm openAddForm() {

        clickButton(toolbar().bottomRight(), "Добавить");

        return N2oSelenide.modal(AttributeForm.class);
    }
}
