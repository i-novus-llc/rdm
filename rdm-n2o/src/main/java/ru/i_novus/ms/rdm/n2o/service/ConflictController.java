package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.rest.client.impl.ReferenceServiceRestClient;

@Controller
public class ConflictController {

    private final ReferenceServiceRestClient referenceService;

    @Autowired
    public ConflictController(ReferenceServiceRestClient referenceService) {

        this.referenceService = referenceService;
    }

    /**
     * Обновление ссылок версии справочника по первичным ключам.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     * @param optLockValue      значение оптимистической блокировки версии
     */
    @SuppressWarnings("unused")
    void refreshReferrer(Integer referrerVersionId, Integer optLockValue) {
        referenceService.refreshReferrer(referrerVersionId, optLockValue);
    }
}
