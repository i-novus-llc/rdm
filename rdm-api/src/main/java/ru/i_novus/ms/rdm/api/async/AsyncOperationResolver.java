package ru.i_novus.ms.rdm.api.async;

import java.io.Serializable;

public interface AsyncOperationResolver {

    boolean isSatisfied(AsyncOperationTypeEnum operationType);

    Serializable resolve(String code, Serializable[] args);
}
