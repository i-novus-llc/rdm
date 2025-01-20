package ru.i_novus.ms.rdm.impl.provider;

import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;

/**
 * Операция после публикации: Обработчик с выбором исполнителя операции.
 */
public interface PostPublishResolverHandler {

    void handle(PostPublishRequest request);
}
