package ru.inovus.ms.rdm.esnsi;

public class EsnsiSyncException extends RuntimeException {

    public EsnsiSyncException(Throwable cause) {
        super(cause);
    }

    public EsnsiSyncException(String msg) {
        super(msg);
    }

}
