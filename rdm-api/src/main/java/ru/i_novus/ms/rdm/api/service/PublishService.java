package ru.i_novus.ms.rdm.api.service;

import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;

/**
 * Публикация справочника: Сервис.
 */
public interface PublishService {

    /**
     * Публикация справочника.
     *
     * @param draftId идентификатор публикуемого черновика
     * @param request запрос на публикацию
     */
    void publish(Integer draftId, PublishRequest request);
}
