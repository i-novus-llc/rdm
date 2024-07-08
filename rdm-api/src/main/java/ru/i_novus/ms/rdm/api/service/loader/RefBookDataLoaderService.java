package ru.i_novus.ms.rdm.api.service.loader;

import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataResponse;

/**
 * Загрузка справочника: Сервис.
 */
public interface RefBookDataLoaderService {

    RefBookDataResponse createAndPublish(RefBookDataRequest request);

    RefBookDataResponse createOrUpdate(RefBookDataRequest request);
}
