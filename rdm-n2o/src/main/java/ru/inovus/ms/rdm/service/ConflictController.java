package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.service.api.ConflictService;

@Controller
public class ConflictController {

    private ConflictService conflictService;

    @Autowired
    public ConflictController(ConflictService conflictService) {
        this.conflictService = conflictService;
    }

    boolean hasTypedConflict(Integer referrerVersionId, ConflictType conflictType) {
        return conflictService.hasTypedConflict(referrerVersionId, conflictType);
    }
}
