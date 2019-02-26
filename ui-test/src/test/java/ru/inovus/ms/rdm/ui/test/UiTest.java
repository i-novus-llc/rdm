package ru.inovus.ms.rdm.ui.test;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import org.junit.BeforeClass;
import org.junit.Test;

public class UiTest {

    private static String appUrl = "http://localhost:8081/";

    @BeforeClass
    public static void init(){
        Configuration.browser = "chrome";
        Configuration.timeout = 25000;
        String urlFromProperty = System.getProperty("appUrl");
        if(urlFromProperty != null) {
            appUrl = urlFromProperty;
        }
    }

    @Test
    public void testOpenStartPage() throws Exception {
        Selenide.open(appUrl);
        Selenide.sleep(3000);
        Selenide.open(appUrl);

        Selenide.$(Selectors.byId("username")).setValue("rdm");
        Selenide.$(Selectors.byId("password")).setValue("rdm").pressEnter();
        Selenide.$(Selectors.byText("Код")).shouldHave(Condition.exist);
        Selenide.$(Selectors.byText("Версия")).shouldHave(Condition.exist);

    }
}
