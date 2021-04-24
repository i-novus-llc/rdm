package ru.inovus.ms.rdm.ui.test.custom.wrapper;

import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.component.button.StandardButton;

public class StandardButtonWrapper {

    private final StandardButton standardButton;

    public StandardButtonWrapper(StandardButton standardButton) {
        this.standardButton = standardButton;
    }

    public void click() {
        standardButton.click();
    }

    public void waitUntil(Condition condition, long timeoutMillis) {
        standardButton.element().waitUntil(condition, timeoutMillis);
    }
}
