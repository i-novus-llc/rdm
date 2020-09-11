package ru.i_novus.ms.rdm.api.async;

public enum AsyncOperation {

    PUBLICATION(Void.TYPE);

    private final Class<?> resultClass;
    AsyncOperation(Class<?> resultClass) {
        this.resultClass = resultClass;
    }

    public Class<?> getResultClass() {
        return resultClass;
    }

}