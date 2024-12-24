package ru.i_novus.ms.rdm.async.api.provider;

import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;

import java.io.Serializable;
import java.util.Map;

/**
 * Асинхронная операция: Обработчик с выбором исполнителя операции.
 */
public interface AsyncOperationResolverHandler {

    Map<String, Serializable> handle(AsyncOperationMessage message);
}
