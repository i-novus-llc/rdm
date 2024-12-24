package ru.i_novus.ms.rdm.async.api.service;

import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;

import java.io.Serializable;
import java.util.UUID;

public interface AsyncOperationMessageService {

    AsyncOperationMessage create(AsyncOperationTypeEnum operationType, String code, Serializable[] args);

    AsyncOperationLogEntry receive(AsyncOperationMessage message);

    UUID send(AsyncOperationTypeEnum operationType, String code, Serializable[] args);
}
