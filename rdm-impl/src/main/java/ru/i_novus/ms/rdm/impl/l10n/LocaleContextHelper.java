package ru.i_novus.ms.rdm.impl.l10n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public class LocaleContextHelper {

    private LocaleContextHelper() {
        throw new UnsupportedOperationException();

    }

    public static Locale getLocale() {

        LocaleContext context = LocaleContextHolder.getLocaleContext();
        if (context == null)
            return new Locale("");

        return context.getLocale();
    }
}
