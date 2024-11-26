package ru.inovus.ms.rdm.ui.test.page;

import com.codeborne.selenide.*;
import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.component.region.RegionItems;
import net.n2oapp.framework.autotest.impl.component.region.N2oTabsRegion;
import org.openqa.selenium.WebElement;

import javax.annotation.Nonnull;
import java.util.stream.StreamSupport;

public class CustomTabItem extends N2oTabsRegion.N2oTabItem {

    public CustomTabItem(SelenideElement element) {
        super(element);
    }

    @Override
    public RegionItems content() {
        return content(".nested-content");
    }

    public RegionItems content(String className) {
        SelenideElement elm = element().parent().parent().parent().$$(".tabs__content--single")
                .findBy(Condition.cssClass("active"));

        ElementsCollection nestingElements = elm.$$(".tabs__content--single.active .tabs__content--single.active > div > " + className);
        ElementsCollection firstLevelElements = elm.$$(".tabs__content--single.active > div > " + className)
                .filter(new WebElementCondition("shouldBeFirstLevelElement") {
                    @Nonnull
                    @Override
                    public CheckResult check(Driver driver, WebElement element) {
                        boolean result = StreamSupport.stream(nestingElements.spliterator(), false).noneMatch(element::equals);
                        return new CheckResult(result ? CheckResult.Verdict.ACCEPT : CheckResult.Verdict.REJECT, null);
                    }
                });
        return N2oSelenide.collection(firstLevelElements, RegionItems.class);
    }
}
