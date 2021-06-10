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

public class RefBookEditPage extends N2oStandardPage {

    @Override
    public void shouldExists() {
        //чтобы не кастомизировать таймаут сначало ждем основную страницу а потом табы
        super.shouldExists();
        getTabsRegion().shouldExists();
        getTabsRegion().tab(Condition.text("Структура")).shouldExists();
    }

    public StructureListWidget structure() {
        TabsRegion.TabItem tabItem = getTabsRegion().tab(Condition.text("Структура"));
        tabItem.click();
        return tabItem.content().widget(StructureListWidget.class);
    }

    public DataListWidget data() {
        TabsRegion.TabItem tabItem = getTabsRegion().tab(Condition.text("Данные"));
        tabItem.click();
        return tabItem.content().widget(DataListWidget.class);
    }

    public void publish() {
        N2oSimplePage n2oSimplePage = page(N2oSimplePage.class);
        N2oDropdownButton actionsButton = n2oSimplePage.widget(N2oFormWidget.class).toolbar().bottomLeft()
                .button("Действия", N2oDropdownButton.class);
        actionsButton.click();
        actionsButton.menuItem("Опубликовать").click();
        Page.Dialog publishDialog = n2oSimplePage.dialog("Публикация справочника");
        publishDialog.shouldBeVisible();
        n2oSimplePage.dialog("Публикация справочника").click("Опубликовать");
    }

    private N2oTabsRegion getTabsRegion() {
        return regions()
                .region(Condition.cssClass("n2o-tabs-region"), N2oTabsRegion.class);
    }

}
