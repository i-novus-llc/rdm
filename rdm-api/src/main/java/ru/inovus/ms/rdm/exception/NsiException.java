package ru.inovus.ms.rdm.exception;

/**
 * Created by tnurdinov on 28.06.2018.
 */
public class NsiException extends RuntimeException {

    public NsiException() {
    }

    public NsiException(String message) {
        super(message);
    }

    public NsiException(String message, Throwable cause) {
        super(message, cause);
    }

    public NsiException(Throwable cause) {
        super(cause);
    }

    public NsiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
