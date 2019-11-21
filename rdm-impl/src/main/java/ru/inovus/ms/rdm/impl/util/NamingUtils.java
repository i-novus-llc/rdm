package ru.inovus.ms.rdm.impl.util;

import net.n2oapp.platform.i18n.UserException;

import java.util.regex.Pattern;

public final class NamingUtils {

    private static final Pattern REFBOOK_CODE = Pattern.compile("[A-Za-z][0-9A-Za-z\\-._]{0,49}");

    private NamingUtils() {throw new UnsupportedOperationException();}

    public static void checkRefBookCode(String name) {
        if (!REFBOOK_CODE.matcher(name).matches())
            throw new UserException("refbook.name-not-valid");
    }

}
