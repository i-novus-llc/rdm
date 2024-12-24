package ru.i_novus.ms.rdm.async.api.provider;

import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;

import java.util.Map;
import java.util.UUID;

/**
 * Асинхронная операция: Обработчик с выбором отправителя операции.
 */
public interface AsyncOperationSenderHandler {

    Map<String, UUID> handle(AsyncOperationMessage message);
}
