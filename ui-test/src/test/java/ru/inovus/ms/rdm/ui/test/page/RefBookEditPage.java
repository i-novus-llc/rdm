package ru.inovus.ms.rdm.ui.test.page;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.api.component.region.TabsRegion;
import net.n2oapp.framework.autotest.impl.component.button.N2oDropdownButton;
import net.n2oapp.framework.autotest.impl.component.page.N2oSimplePage;
import net.n2oapp.framework.autotest.impl.component.page.N2oStandardPage;
import net.n2oapp.framework.autotest.impl.component.widget.N2oFormWidget;

import static net.n2oapp.framework.autotest.N2oSelenide.page;

/**
 * Страница редактирования справочника.
 */
public class RefBookEditPage extends N2oStandardPage {

    private static final String DATA_GRID_CLASS_NAME = ".rdm-data-grid";

    @Override
    public void shouldExists() {

        // Чтобы не кастомизировать таймаут, сначала ждём основную страницу, а потом вкладки.
        super.shouldExists();

        getTabsRegion().shouldExists();
        getTab("Структура").shouldExists();
    }

    public StructureWidget structure() {

        final TabsRegion.TabItem tabItem = getTab("Структура");
        tabItem.click();
        tabItem.shouldBeActive();

        return tabItem.content().widget(StructureWidget.class);
    }

    public DataTableWidget dataTable() {

        final CustomTabItem tabItem = getTab("Данные");
        tabItem.shouldExists();
        tabItem.click();
        tabItem.shouldBeActive();

        return tabItem.content(DATA_GRID_CLASS_NAME).widget(DataTableWidget.class);
    }

    public DataTableWithConflictsWidget dataTableWithConflicts() {

        final CustomTabItem tabItem = getTab("Данные с конфликтами");
        tabItem.click();
        tabItem.shouldBeActive();

        return tabItem.content(DATA_GRID_CLASS_NAME).widget(DataTableWithConflictsWidget.class);
    }

    public void publish() {

        final N2oSimplePage n2oSimplePage = page(N2oSimplePage.class);
        final N2oDropdownButton actionsButton = getActionsButton(n2oSimplePage);
        actionsButton.menuItem("Опубликовать").click();

        final Page.Dialog n2oDialog = n2oSimplePage.dialog("Публикация справочника");
        n2oDialog.shouldBeVisible();
        n2oDialog.button("Опубликовать").click();
    }

    public void refreshReferrer() {

        final N2oSimplePage n2oSimplePage = page(N2oSimplePage.class);
        final N2oDropdownButton actionsButton = getActionsButton(n2oSimplePage);
        actionsButton.menuItem("Обновить ссылки").click();

        final Page.Dialog n2oDialog = n2oSimplePage.dialog("Обновить ссылки");
        n2oDialog.shouldBeVisible();
        n2oDialog.button("Да").click();
    }

    /** Регион со вкладками. */
    // NB: После доработки n2o вернуть оригинальный N2oTabsRegion
    private CustomTabsRegion getTabsRegion() {
        return regions().region(Condition.cssClass("n2o-tabs-region"), CustomTabsRegion.class);
    }

    /** Вкладка с указанным заголовком. */
    // NB: После доработки n2o вернуть оригинальный TabsRegion.TabItem
    private CustomTabItem getTab(String label) {
        return getTabsRegion().tab(Condition.text(label));
    }

    /** Меню "Действия". */
    private N2oDropdownButton getActionsButton(N2oSimplePage n2oSimplePage) {

        final N2oDropdownButton actionsButton = n2oSimplePage.widget(N2oFormWidget.class).toolbar().bottomLeft()
                .button("Действия", N2oDropdownButton.class);
        actionsButton.click();

        return actionsButton;
    }
}
