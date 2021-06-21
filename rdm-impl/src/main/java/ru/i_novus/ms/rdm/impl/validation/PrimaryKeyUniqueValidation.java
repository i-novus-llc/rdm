package ru.i_novus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.Collections;
import java.util.List;

/**
 * Валидация уникальности значений по указанным наименованиям полей в записях хранилища.
 */
public class PrimaryKeyUniqueValidation implements RdmValidation {

    private final DraftDataService draftDataService;

    /** Код хранилища. */
    private final String storageCode;

    /** Наименования проверяемых полей. */
    private final List<String> primaryNames;

    public PrimaryKeyUniqueValidation(DraftDataService draftDataService,
                                      String storageCode, List<String> primaryNames) {
        this.draftDataService = draftDataService;

        this.storageCode = storageCode;
        this.primaryNames = primaryNames;
    }

    @Override
    public List<Message> validate() {

        if(!draftDataService.isUnique(storageCode, primaryNames))
            return Collections.singletonList(new Message("primary.key.not.unique"));

        return Collections.emptyList();
    }
}