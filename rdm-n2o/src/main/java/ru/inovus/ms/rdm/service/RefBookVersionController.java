package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.PublishService;

@Controller
public class RefBookVersionController {

    @Autowired
    private PublishService publishService;

    @Autowired
    private ConflictService conflictService;

    public void publishDraft(Integer draftId, boolean processResolvableConflicts) {
        publishService.publish(draftId, null, null, null, processResolvableConflicts);
    }

    public void refreshReferrer(Integer referrerVersionId) {
        conflictService.refreshReferrerByPrimary(referrerVersionId);
    }
}
