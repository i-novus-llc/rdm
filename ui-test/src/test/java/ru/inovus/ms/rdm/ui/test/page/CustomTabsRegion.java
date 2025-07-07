package ru.inovus.ms.rdm.ui.test.page;

import com.codeborne.selenide.WebElementCondition;
import net.n2oapp.framework.autotest.impl.component.region.N2oTabsRegion;

public class CustomTabsRegion extends N2oTabsRegion {

    @Override
    public CustomTabItem tab(WebElementCondition by) {
        return new CustomTabItem(navItem().findBy(by));
    }
}
