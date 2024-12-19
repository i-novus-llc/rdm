package ru.inovus.ms.rdm.ui.test.util;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.component.DropDown;
import net.n2oapp.framework.autotest.api.component.control.Control;
import net.n2oapp.framework.autotest.api.component.control.InputText;
import net.n2oapp.framework.autotest.api.component.control.TextArea;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.impl.component.control.N2oCheckbox;
import net.n2oapp.framework.autotest.impl.component.control.N2oDateInput;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oSelect;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public final class UiTestUtil {

    private static final ZoneId UNIVERSAL_TIMEZONE = ZoneId.of("UTC");

    private static final long DEFAULT_SLEEP_TIME = TimeUnit.SECONDS.toMillis(1);

    private UiTestUtil() {
        // Nothing to do.
    }

    /**
     * Открытие страницы с полным url без query-параметров.
     *
     * @param clazz   класс страницы
     * @param pageUrl url страницы
     * @return Страница
     */
    public static <T extends Page> T openPage(Class<T> clazz, String pageUrl) {
        return N2oSelenide.open(pageUrl, clazz);
    }

    public static void fillInputControl(Control control, String value) {

        control.shouldExists();
        fillControlValue(control, value);
        control.shouldHaveValue(value);
    }

    public static void fillControlValue(Control control, String value) {

        if (control instanceof InputText inputText) {
            inputText.setValue(value);

        } else if (control instanceof N2oSelect n2oSelect) {
            fillN2oSelectValue(n2oSelect, value);

        } else if (control instanceof N2oInputSelect n2oInputSelect) {
            fillN2oInputSelectValue(n2oInputSelect, value);

        } else if (control instanceof TextArea textArea) {
            textArea.setValue(value);

        } else if (control instanceof N2oDateInput n2oDateInput) {
            n2oDateInput.setValue(value);

        } else {
            throw new IllegalArgumentException("Control is not for input");
        }
    }

    public static void fillN2oSelectValue(N2oSelect control, String value) {

        control.openPopup();
        final DropDown dropDown = control.dropdown();
        dropDown.selectItemBy(Condition.text(value));
        control.closePopup();

        control.shouldHaveValue(value);
    }

    public static void fillN2oInputSelectValue(N2oInputSelect control, String value) {

        control.openPopup();
        final DropDown dropDown = control.dropdown();
        dropDown.selectItemBy(Condition.text(value));
        control.closePopup();

        control.shouldHaveValue(value);
    }

    public static void fillCheckBox(Control checkBox, boolean value) {

        checkBox.shouldExists();

        if (!(checkBox instanceof N2oCheckbox control))
            throw new IllegalArgumentException("Control is not check box");

        control.setChecked(value);

        if (value) {
            control.shouldBeChecked();
        } else {
            control.shouldBeEmpty();
        }
    }

    /**
     * Получение текущего времени в UTC.
     *
     * @return Текущее время
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UNIVERSAL_TIMEZONE);
    }

    /**
     * Ожидание результата действия.
     *
     * @param milliseconds Время ожидания (мс)
     */
    public static void waitActionResult(long milliseconds) {

        Selenide.sleep(milliseconds != 0 ? milliseconds : DEFAULT_SLEEP_TIME);
    }

    //private static String getRestExceptionMessage(RestException re) {
    //
    //    if (!StringUtils.isEmpty(re.getMessage()))
    //        return re.getMessage();
    //
    //    if (!isEmpty(re.getErrors()))
    //        return re.getErrors().get(0).getMessage();
    //
    //    return null;
    //}
}
