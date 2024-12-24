package ru.i_novus.ms.rdm.async.api.provider;

import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;

import java.util.UUID;

/**
 * Асинхронная операция: Отправка информации.
 * <p>
 * Внимание:
 * Сервисы необходимо подключать через setter'ы с @Lazy-аргументом,
 * чтобы исключить появление циклических зависимостей.
 */
public interface AsyncOperationSender {

    String getName();

    boolean isSatisfied(AsyncOperationTypeEnum operationType);

    UUID send(AsyncOperationMessage message);
}
