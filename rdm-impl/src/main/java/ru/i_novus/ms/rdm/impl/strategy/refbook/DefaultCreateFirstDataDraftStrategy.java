package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import static java.util.Collections.emptyList;

@Component
public class DefaultCreateFirstDataDraftStrategy implements CreateFirstDataDraftStrategy {

    @Autowired
    private DraftDataService draftDataService;

    @Override
    public String create() {
        return draftDataService.createDraft(emptyList());
    }

    protected DraftDataService getDraftDataService() {
        return draftDataService;
    }
}
