package ru.inovus.ms.rdm.ui.test.page;


import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.impl.component.button.N2oDropdownButton;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputText;
import net.n2oapp.framework.autotest.impl.component.page.N2oSimplePage;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import java.util.List;

/**
 * Страница "Реестр НСИ".
 */
public class RefBookListPage extends N2oSimplePage {

    @Override
    public void shouldExists() {

        final N2oTableWidget n2oTableWidget = table();
        n2oTableWidget.shouldExists();
        n2oTableWidget.filters().shouldBeVisible();
    }

    public CreateRefBookWidget openCreateRefBookPage() {

        final N2oDropdownButton createRefBookButton = table().toolbar().topLeft()
                .button("Создать справочник", N2oDropdownButton.class);
        createRefBookButton.click();
        createRefBookButton.menuItem("Создать справочник").click();

        CreateRefBookWidget createWidget = widget(CreateRefBookWidget.class);
        createWidget.setOpenedFromPage(this);
        return createWidget;
    }

    public RefBookEditPage openRefBookEditPage(int rowNum) {

        table().columns().rows().row(rowNum).click();
        table().toolbar().topLeft().button("Изменить справочник").click();

        return N2oSelenide.page(RefBookEditPage.class);
    }

    public N2oInputText codeFilter() {
        return table().filters().fields().field("Код справочника").control(N2oInputText.class);
    }

    public N2oInputText nameFilter() {
        return table().filters().fields().field("Наименование справочника").control(N2oInputText.class);
    }

    public void search() {
        table().filters().toolbar().button("Найти").click();
    }

    public void rowShouldHaveTexts(int columnNumber, List<String> text) {
        table().columns().rows().columnShouldHaveTexts(columnNumber, text);
    }

    public void rowShouldHaveSize(int size) {
        table().columns().rows().shouldHaveSize(size);
    }

    public void deleteRow(int rowNumber) {

        table().columns().rows().row(rowNumber).click();
        table().toolbar().topLeft().button("Удалить справочник").click();

        final Page.Dialog deleteDialog = dialog("Удалить");
        deleteDialog.shouldBeVisible();
        deleteDialog.button("Да").click();
    }

    private N2oTableWidget table() {
        return widget(N2oTableWidget.class);
    }
}
