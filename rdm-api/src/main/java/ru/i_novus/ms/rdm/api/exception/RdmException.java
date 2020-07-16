package ru.i_novus.ms.rdm.api.exception;

/**
 * Created by tnurdinov on 28.06.2018.
 */
public class RdmException extends RuntimeException {

    public RdmException() {
    }

    public RdmException(String message) {
        super(message);
    }

    public RdmException(String message, Throwable cause) {
        super(message, cause);
    }

    public RdmException(Throwable cause) {
        super(cause);
    }

    public RdmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
