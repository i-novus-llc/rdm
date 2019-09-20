package ru.inovus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.model.version.VersionCriteria;

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
