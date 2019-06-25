package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.PublishService;

@Controller
public class RefBookVersionController {

    private PublishService publishService;
    private ConflictService conflictService;

    @Autowired
    public RefBookVersionController(PublishService publishService, ConflictService conflictService) {
        this.publishService = publishService;
        this.conflictService = conflictService;
    }

    public void publishDraft(Integer draftId, boolean processResolvableConflicts) {
        publishService.publish(draftId, null, null, null, processResolvableConflicts);
    }

    public void refreshReferrer(Integer referrerVersionId) {
        conflictService.refreshReferrerByPrimary(referrerVersionId);
    }
}
