package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.service.VersionService;

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