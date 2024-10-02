package ru.i_novus.ms.rdm.impl.util;

import jakarta.persistence.PersistenceException;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;

import java.sql.SQLException;

public final class ErrorUtil {

    private static final String ROW_NOT_UNIQUE = "row.not.unique";

    private static final String PSQL_UNIQUE_VIOLATION_ERROR_CODE = "23505";

    private ErrorUtil() {
        // Nothing to do.
    }

    public static void rethrowError(RuntimeException e) {

        if (e instanceof NotUniqueException) {
            throw new UserException(ROW_NOT_UNIQUE, e);

        } else if (e instanceof PersistenceException pe) {

            throw transformException(pe);
        }
    }

    /* Адаптированные методы из DraftDataServiceImpl. */

    /** Преобразование ошибки хранилища в исключение. */
    private static RuntimeException transformException(PersistenceException exception) {

        final SQLException sqlException = getSQLException(exception);

        // Обработка кода ошибки о нарушении уникальности в PostgreSQL.
        if (sqlException != null &&
                PSQL_UNIQUE_VIOLATION_ERROR_CODE.equals(sqlException.getSQLState())) {
            return new UserException(ROW_NOT_UNIQUE);
        }

        return exception;
    }

    /** Получение sql-ошибки из ошибки хранилища. */
    private static SQLException getSQLException(PersistenceException exception) {

        Throwable cause = exception;
        while (cause != null && !(cause instanceof SQLException)) {
            cause = cause.getCause();
        }

        return (SQLException) cause;
    }
}
