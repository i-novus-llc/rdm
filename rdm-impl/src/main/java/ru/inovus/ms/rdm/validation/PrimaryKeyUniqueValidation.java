package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.Collections;
import java.util.List;

public class PrimaryKeyUniqueValidation implements RdmValidation {

    private DraftDataService draftDataService;
    private String storageCode;
    private List<String> primaryAttributeNames;

    public PrimaryKeyUniqueValidation(DraftDataService draftDataService, String storageCode, List<String> primaryAttributeNames) {
        this.draftDataService = draftDataService;
        this.storageCode = storageCode;
        this.primaryAttributeNames = primaryAttributeNames;
    }

    @Override
    public List<Message> validate() {
        if(!draftDataService.isUnique(storageCode, primaryAttributeNames))
            return Collections.singletonList(new Message("primary.key.not.unique"));
        return Collections.emptyList();
    }
}