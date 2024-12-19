package ru.inovus.ms.rdm.ui.test.page;

import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.impl.component.page.N2oPage;

import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selectors.byId;
import static com.codeborne.selenide.Selenide.$;
import static ru.inovus.ms.rdm.ui.test.util.UiTestUtil.waitActionResult;

/**
 * Страница логина.
 */
public class RdmLoginPage extends N2oPage {

    private static final long WAIT_FILL_INPUT_TIME = TimeUnit.SECONDS.toMillis(1);

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
        //$(byId(field)).type(value);
        waitActionResult(WAIT_FILL_INPUT_TIME);
    }
}
