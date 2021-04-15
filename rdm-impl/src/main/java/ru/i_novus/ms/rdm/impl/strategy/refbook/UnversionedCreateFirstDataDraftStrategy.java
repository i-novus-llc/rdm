package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

@Component
public class UnversionedCreateFirstDataDraftStrategy extends DefaultCreateFirstDataDraftStrategy {

    @Override
    public String create() {

        String storageCode = super.create();
        getDraftDataService().applyDraft(null, storageCode, TimeUtils.now());

        return storageCode;
    }
}
