package ru.i_novus.ms.rdm.impl.service;

import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;

/**
 * Сервис для операций после публикации справочника.
 */
public interface PostPublishService {

    void process(PostPublishRequest request);
}
