package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.List;

public class PrimaryKeyUniqueValidation implements RdmValidation {

    private DraftDataService draftDataService;
    private String storageCode;
    private List<String> fieldNames;

    public PrimaryKeyUniqueValidation(DraftDataService draftDataService, String storageCode, List<String> fieldNames) {
        this.draftDataService = draftDataService;
        this.storageCode = storageCode;
        this.fieldNames = fieldNames;
    }

    @Override
    public Message validate() {
        if(!draftDataService.isUnique(storageCode, fieldNames))
            return new Message("primary.key.not.unique");
        return null;
    }
}