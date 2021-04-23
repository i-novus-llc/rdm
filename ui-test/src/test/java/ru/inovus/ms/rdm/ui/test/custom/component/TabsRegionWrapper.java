package ru.inovus.ms.rdm.ui.test.custom.component;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.impl.component.region.N2oTabsRegion;

public class TabsRegionWrapper {

    private final N2oTabsRegion n2oTabsRegion;

    public TabsRegionWrapper(N2oTabsRegion n2oTabsRegion) {
        this.n2oTabsRegion = n2oTabsRegion;
    }

    public void waitUntil(Condition condition, long timeoutMillis) {
        n2oTabsRegion.element().waitUntil(condition, timeoutMillis);
    }
}
