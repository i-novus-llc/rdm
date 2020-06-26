package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.model.version.VersionCriteria;

@Controller
public class VersionController {

    private static final String ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE = "action.draft.was.changed";

    @Autowired
    private VersionService versionService;

    /**
     * Получение версии справочника для открытия на просмотр/редактирование.
     */
    @SuppressWarnings("unused")
    public RefBookVersion getVersion(VersionCriteria criteria) {

        // NB: criteria.getExcludeDraft() ignored now.
        RefBookVersion version = versionService.getById(criteria.getId());

        Integer optLockValue = criteria.getOptLockValue();
        if (optLockValue != null && !optLockValue.equals(version.getOptLockValue())) {
            throw new UserException(new Message(ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE));
        }

        return version;
    }
}
