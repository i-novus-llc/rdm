package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.model.version.VersionCriteria;
import ru.inovus.ms.rdm.service.api.VersionService;

@Controller
public class VersionController {

    @Autowired
    private VersionService versionService;

    /**
     * Получение версии справочника для открытия на просмотр/редактирование.
     */
    @SuppressWarnings("unused")
    public RefBookVersion getVersion(VersionCriteria criteria) {

        // NB: criteria.getExcludeDraft() ignored now.
        return versionService.getById(criteria.getId());
    }
}
