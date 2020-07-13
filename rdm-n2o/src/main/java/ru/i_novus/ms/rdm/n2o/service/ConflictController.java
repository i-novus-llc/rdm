package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.service.ReferenceService;

@Controller
public class ConflictController {

    private ReferenceService referenceService;

    @Autowired
    public ConflictController(ReferenceService referenceService) {
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
