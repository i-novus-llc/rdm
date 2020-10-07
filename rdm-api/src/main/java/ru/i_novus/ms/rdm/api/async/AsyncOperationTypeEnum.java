package ru.i_novus.ms.rdm.api.async;

import java.io.Serializable;

/**
 * Асинхронная операция: Тип операции.
 */
public enum AsyncOperationTypeEnum {

    PUBLICATION(AsyncVoidResult.class),
    L10N_PUBLICATION(AsyncVoidResult.class);

    private final Class<? extends Serializable> resultClass;

    AsyncOperationTypeEnum(Class<? extends Serializable> resultClass) {
        this.resultClass = resultClass;
    }

    public Class<? extends Serializable> getResultClass() {
        return resultClass;
    }
}
