package ru.i_novus.ms.rdm.impl.provider;

import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;

/**
 * Операция после публикации: Исполнитель операции.
 * <p>
 * Внимание:
 * Сервисы необходимо подключать через setter'ы с @Lazy-аргументом,
 * чтобы исключить появление циклических зависимостей.
 */
public interface PostPublishResolver {

    void resolve(PostPublishRequest request);
}
