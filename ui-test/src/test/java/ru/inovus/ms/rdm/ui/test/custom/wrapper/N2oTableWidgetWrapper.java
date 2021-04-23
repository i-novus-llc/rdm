package ru.inovus.ms.rdm.ui.test.custom.wrapper;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.component.widget.table.TableWidget;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;

import static com.codeborne.selenide.Selectors.byClassName;
import static com.codeborne.selenide.Selenide.$;

public class N2oTableWidgetWrapper {

    private final N2oTableWidget n2oTableWidget;

    public N2oTableWidgetWrapper(N2oTableWidget n2oTableWidget) {
        this.n2oTableWidget = n2oTableWidget;
    }

    public N2oTableWidget original() {
        return n2oTableWidget;
    }

    public void waitUntilFilterVisible(Condition condition, long timeoutMillis) {
        n2oTableWidget.element().$(".n2o-filter").waitUntil(condition, timeoutMillis);
    }

    public void waitUntilTableContentLoaded(Condition condition, long timeoutMillis) {
        $(byClassName("n2o-advanced-table-row")).waitUntil(condition, timeoutMillis);
    }

    public TableWidget.Filters filters() {
        return n2oTableWidget.filters();
    }

    public TableWidget.Columns columns() {
        return n2oTableWidget.columns();
    }

    public TableWidget.WidgetToolbar toolbar() {
        return n2oTableWidget.toolbar();
    }

    public void shouldExists() {
        n2oTableWidget.shouldExists();
    }
}
