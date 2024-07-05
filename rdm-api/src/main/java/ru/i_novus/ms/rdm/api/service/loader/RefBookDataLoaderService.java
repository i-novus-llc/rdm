package ru.i_novus.ms.rdm.api.service.loader;

import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;

/**
 * Загрузка справочника: Сервис.
 */
public interface RefBookDataLoaderService {

    boolean createAndPublish(RefBookDataRequest request);

    boolean createOrUpdate(RefBookDataRequest request);
}
