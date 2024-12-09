package ru.i_novus.ms.rdm.api.async;

import java.util.Map;
import java.util.UUID;

/**
 * Асинхронная операция: Обработчик с выбором отправителя операции.
 */
public interface AsyncOperationSenderHandler {

    Map<String, UUID> handle(AsyncOperationMessage message);
}
