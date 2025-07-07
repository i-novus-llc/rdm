package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.api.collection.Toolbar;
import net.n2oapp.framework.autotest.api.component.button.StandardButton;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import java.util.List;

/**
 * Таблица на вкладке версии справочника.
 */
abstract class RefBookEditTableWidget extends N2oTableWidget {

    public void selectRow(int rowNum) {
        columns().rows().row(rowNum).click();
    }

    public void rowShouldHaveSize(int size) {
        columns().rows().shouldHaveSize(size);
    }

    public void columnShouldHaveTexts(int index, List<String> texts) {
        columns().rows().columnShouldHaveTexts(index, texts);
    }

    public void clickButton(Toolbar toolbar, String label) {

        final StandardButton button = toolbar.button(label);
        shouldClicked(button);
        button.click();
    }

    public void shouldClicked(StandardButton button) {

        button.shouldExists();
        button.shouldBeVisible();
        button.shouldBeEnabled();
    }
}
