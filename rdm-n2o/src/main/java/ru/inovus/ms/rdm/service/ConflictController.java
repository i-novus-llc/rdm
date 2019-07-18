package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.service.api.ConflictService;

@Controller
public class ConflictController {

    private ConflictService conflictService;

    @Autowired
    public ConflictController(ConflictService conflictService) {
        this.conflictService = conflictService;
    }

    /**
     * Обновление ссылок версии справочника по первичным ключам.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     */
    @SuppressWarnings("unused")
    void refreshReferrer(Integer referrerVersionId) {
        conflictService.refreshReferrerByPrimary(referrerVersionId);
    }
}
