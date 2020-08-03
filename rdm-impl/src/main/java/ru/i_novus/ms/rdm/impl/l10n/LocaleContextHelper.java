package ru.i_novus.ms.rdm.impl.l10n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public class LocaleContextHelper {

    private static final Locale EMPTY_LOCALE = new Locale("");

    private LocaleContextHelper() {
        throw new UnsupportedOperationException();

    }

    public static Locale getLocale() {

        LocaleContext context = LocaleContextHolder.getLocaleContext();
        if (context == null)
            return EMPTY_LOCALE;

        return context.getLocale();
    }

    public static void setLocale(String code) {

        Locale locale = code != null ? new Locale(code) : EMPTY_LOCALE;
        LocaleContextHolder.setLocale(locale);
    }
}
