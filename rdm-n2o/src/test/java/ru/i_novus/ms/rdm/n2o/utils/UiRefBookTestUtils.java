package ru.i_novus.ms.rdm.n2o.utils;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;

import java.util.function.BiConsumer;

import static org.springframework.util.CollectionUtils.isEmpty;

@SuppressWarnings("java:S3740")
public class UiRefBookTestUtils {

    private static final String WAITING_ERROR = "Ожидается ошибка ";

    private UiRefBookTestUtils() {
        // Nothing to do.
    }

    /**
     * Сравнение объектов с учётом хеша и преобразования в строку.
     */
    public static void assertObjects(BiConsumer<Object, Object> objectAssert, Object current, Object actual) {

        objectAssert.accept(current, actual);

        if (current != null && actual != null) {
            objectAssert.accept(current.hashCode(), actual.hashCode());
            objectAssert.accept(current.toString(), actual.toString());
        }
    }

    /**
     * Получение сообщения об ожидании исключения.
     */
    public static String getFailedMessage(Class expectedExceptionClass) {

        return WAITING_ERROR + expectedExceptionClass.getSimpleName();
    }

    /**
     * Получение кода сообщения об ошибке из исключения.
     */
    public static String getExceptionMessage(Exception e) {

        if (e instanceof UserException) {
            UserException ue = (UserException) e;

            if (!isEmpty(ue.getMessages()))
                return ue.getMessages().get(0).getCode();
        }

        if (!StringUtils.isEmpty(e.getMessage()))
            return e.getMessage();

        return null;
    }
}
