package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;

import static com.codeborne.selenide.Selectors.byId;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class RdmLogin {

    public static void login(String username, String password) {
        open("/");
        fillInput("username", username);
        fillInput("password", password);
        login();

        N2oSelenide.page(RefBookListPage.class);
    }

    private static void login() {
        $(byId("kc-login")).pressEnter();
    }

    private static void fillInput(String field, String value) {
        $(byId(field)).val(value);
    }

}
