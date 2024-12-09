package ru.i_novus.ms.rdm.api.async;

import java.io.Serializable;
import java.util.Map;

/**
 * Асинхронная операция: Обработчик с выбором исполнителя операции.
 */
public interface AsyncOperationResolverHandler {

    Map<String, Serializable> handle(AsyncOperationMessage message);
}
