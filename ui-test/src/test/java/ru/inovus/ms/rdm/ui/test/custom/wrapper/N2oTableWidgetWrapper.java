package ru.inovus.ms.rdm.ui.test.custom.wrapper;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

public class N2oTableWidgetWrapper {

    private final N2oTableWidget n2oTableWidget;

    public N2oTableWidgetWrapper(N2oTableWidget n2oTableWidget) {
        this.n2oTableWidget = n2oTableWidget;
    }

    public void waitUntilFilterVisible(Condition condition, long timeoutMillis) {
        n2oTableWidget.element().$(".n2o-filter").waitUntil(condition, timeoutMillis);
    }
}
