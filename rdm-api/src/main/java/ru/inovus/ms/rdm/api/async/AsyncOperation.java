package ru.inovus.ms.rdm.api.async;

public enum AsyncOperation {

    PUBLICATION(Void.TYPE);

    private final Class<?> resultClass;
    AsyncOperation(Class<?> resultClass) {
        this.resultClass = resultClass;
    }

    public Class<?> getResultClass() {
        return resultClass;
    }

    public enum Status {
        QUEUED, IN_PROGRESS, ERROR, DONE
    }

}
