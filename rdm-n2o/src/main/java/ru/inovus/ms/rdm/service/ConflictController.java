package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.service.api.ReferenceService;

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
     */
    @SuppressWarnings("unused")
    void refreshReferrer(Integer referrerVersionId) {
        referenceService.refreshReferrer(referrerVersionId);
    }
}
