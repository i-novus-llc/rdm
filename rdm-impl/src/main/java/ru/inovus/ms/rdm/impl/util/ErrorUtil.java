package ru.inovus.ms.rdm.impl.util;

import net.n2oapp.platform.i18n.UserException;
import org.postgresql.util.PSQLException;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;

import javax.persistence.PersistenceException;

public class ErrorUtil {

    private static final String ROW_NOT_UNIQUE = "row.not.unique";

    private ErrorUtil() {throw new UnsupportedOperationException();}

    public static void rethrowError(RuntimeException e) {
        if (e instanceof NotUniqueException) {
            throw new UserException(ROW_NOT_UNIQUE, e);
        } else if (e instanceof PersistenceException) {
            boolean notUnique = false;
            if (e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause() instanceof PSQLException)
                notUnique = "23505".equals(((PSQLException) e.getCause().getCause()).getSQLState());
            if (notUnique)
                throw new UserException(ROW_NOT_UNIQUE, e);
            else throw e;
        }
    }

}
