package ru.i_novus.ms.rdm.async.api.provider;

import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;

import java.io.Serializable;

/**
 * Асинхронная операция: Исполнитель операции.
 * <p>
 * Внимание:
 * Сервисы необходимо подключать через setter'ы с @Lazy-аргументом,
 * чтобы исключить появление циклических зависимостей.
 */
public interface AsyncOperationResolver {

    String getName();

    boolean isSatisfied(AsyncOperationTypeEnum operationType);

    Serializable resolve(AsyncOperationMessage message);
}
