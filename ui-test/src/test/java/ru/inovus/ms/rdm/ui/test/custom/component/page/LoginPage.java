package ru.inovus.ms.rdm.ui.test.custom.component.page;

import net.n2oapp.framework.autotest.impl.component.page.N2oPage;

import static com.codeborne.selenide.Selectors.byId;
import static com.codeborne.selenide.Selenide.$;

public class LoginPage extends N2oPage {

    public void login(String username, String password) {
        fillInput("username", username);
        fillInput("password", password);
        login();
    }

    private void login() {
        $(byId("kc-login")).pressEnter();
    }

    private void fillInput(String field, String value) {
        $(byId(field)).val(value);
    }
}
