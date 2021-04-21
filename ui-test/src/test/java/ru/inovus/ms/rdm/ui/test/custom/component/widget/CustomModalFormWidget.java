package ru.inovus.ms.rdm.ui.test.custom.component.widget;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import net.n2oapp.framework.autotest.api.collection.Fields;
import net.n2oapp.framework.autotest.impl.component.widget.N2oFormWidget;

import static com.codeborne.selenide.Selectors.byClassName;
import static com.codeborne.selenide.Selenide.$;

public class CustomModalFormWidget extends N2oFormWidget {

    private final SelenideElement modalContent = $(byClassName("modal-content"));
    private final N2oFormWidget n2oFormWidget = new N2oFormWidget();

    @Override
    public Fields fields() {
        n2oFormWidget.setElement(modalContent);
        return n2oFormWidget.fields();
    }

    public void waitUntil(Condition condition, long timeoutMilliseconds) {
        modalContent.waitUntil(condition, timeoutMilliseconds);
    }

    public void save() {
        button("Сохранить").click();
    }

    private ElementsCollection buttons() {
        return $(byClassName("modal-footer")).$$(byClassName("btn"));
    }

    private SelenideElement button(String label) {
        return buttons().find(Condition.text(label));
    }
}
