package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

@Component
public class DefaultCreateDraftStorageStrategy implements CreateDraftStorageStrategy {

    @Autowired
    private DraftDataService draftDataService;

    @Override
    public String create(Structure structure) {
        return draftDataService.createDraft(ConverterUtil.fields(structure));
    }
}
