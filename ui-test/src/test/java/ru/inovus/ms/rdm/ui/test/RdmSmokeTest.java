package ru.inovus.ms.rdm.ui.test;


import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.N2oSelenide;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.ui.test.login.LoginPage;

class RdmSmokeTest {

    private static final Logger logger = LoggerFactory.getLogger(RdmSmokeTest.class);

    private static final String RDM_BASE_URL = "http://localhost:8080";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    @BeforeAll
    public static void setUp() {
        Configuration.baseUrl = RDM_BASE_URL;
    }

    @AfterAll
    public static void tearDown() {
        Selenide.closeWebDriver();
    }

    @Test
    void test() {
        login();

    }

    private void login() {
        LoginPage loginPage = N2oSelenide.open("/", LoginPage.class);
        loginPage.login(USERNAME, PASSWORD);
        logger.info("User logged in");
    }
}
