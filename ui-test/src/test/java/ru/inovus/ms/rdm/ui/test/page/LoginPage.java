package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.impl.component.page.N2oPage;

import static com.codeborne.selenide.Selectors.byId;
import static com.codeborne.selenide.Selenide.$;

public class LoginPage extends N2oPage {

    public RefBookListPage login(String username, String password) {
        fillInput("username", username);
        fillInput("password", password);
        login();
        return N2oSelenide.page(RefBookListPage.class);
    }

    private void login() {
        $(byId("kc-login")).pressEnter();
    }

    private void fillInput(String field, String value) {
        $(byId(field)).val(value);
    }
}
