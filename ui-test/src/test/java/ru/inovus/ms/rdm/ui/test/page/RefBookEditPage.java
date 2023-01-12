package ru.inovus.ms.rdm.ui.test.page;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.api.component.region.TabsRegion;
import net.n2oapp.framework.autotest.impl.component.button.N2oDropdownButton;
import net.n2oapp.framework.autotest.impl.component.page.N2oSimplePage;
import net.n2oapp.framework.autotest.impl.component.page.N2oStandardPage;
import net.n2oapp.framework.autotest.impl.component.region.N2oTabsRegion;
import net.n2oapp.framework.autotest.impl.component.widget.N2oFormWidget;

import static net.n2oapp.framework.autotest.N2oSelenide.page;

/**
 * Страница редактирования справочника.
 */
public class RefBookEditPage extends N2oStandardPage {

    @Override
    public void shouldExists() {

        // Чтобы не кастомизировать таймаут, сначала ждём основную страницу, а потом вкладки.
        super.shouldExists();

        getTabsRegion().shouldExists();
        getTab("Структура").shouldExists();
    }

    public StructureWidget structure() {

        TabsRegion.TabItem tabItem = getTab("Структура");
        tabItem.click();
        tabItem.shouldBeActive();

        return tabItem.content().widget(StructureWidget.class);
    }

    public DataListWidget data() {

        TabsRegion.TabItem tabItem = getTab("Данные");
        tabItem.click();
        tabItem.shouldBeActive();

        return tabItem.content().widget(DataListWidget.class);
    }

    public DataWithConflictsWidget dataWithConflicts() {

        TabsRegion.TabItem tabItem = getTab("Данные с конфликтами");
        tabItem.click();
        tabItem.shouldBeActive();

        return tabItem.content().widget(DataWithConflictsWidget.class);
    }

    public void publish() {

        N2oSimplePage n2oSimplePage = page(N2oSimplePage.class);
        N2oDropdownButton actionsButton = getActionsButton(n2oSimplePage);
        actionsButton.menuItem("Опубликовать").click();

        Page.Dialog n2oDialog = n2oSimplePage.dialog("Публикация справочника");
        n2oDialog.shouldBeVisible();
        n2oDialog.button("Опубликовать").click();
    }

    public void refreshReferrer() {

        N2oSimplePage n2oSimplePage = page(N2oSimplePage.class);
        N2oDropdownButton actionsButton = getActionsButton(n2oSimplePage);
        actionsButton.menuItem("Обновить ссылки").click();

        Page.Dialog n2oDialog = n2oSimplePage.dialog("Обновить ссылки");
        n2oDialog.shouldBeVisible();
        n2oDialog.button("Да").click();
    }

    /** Регион со вкладками. */
    private N2oTabsRegion getTabsRegion() {
        return regions().region(Condition.cssClass("n2o-tabs-region"), N2oTabsRegion.class);
    }

    /** Вкладка с указанным заголовком. */
    private TabsRegion.TabItem getTab(String label) {
        return getTabsRegion().tab(Condition.text(label));
    }

    /** Меню "Действия". */
    private N2oDropdownButton getActionsButton(N2oSimplePage n2oSimplePage) {

        N2oDropdownButton actionsButton = n2oSimplePage.widget(N2oFormWidget.class).toolbar().bottomLeft()
                .button("Действия", N2oDropdownButton.class);
        actionsButton.click();

        return actionsButton;
    }
}
