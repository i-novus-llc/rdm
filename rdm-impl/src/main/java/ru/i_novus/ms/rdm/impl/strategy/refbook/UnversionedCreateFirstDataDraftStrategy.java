package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

@Component
public class UnversionedCreateFirstDataDraftStrategy extends DefaultCreateFirstDataDraftStrategy {

    @Override
    @Transactional
    public String create() {

        String draftCode = super.create();
        return getDraftDataService().applyDraftItself(draftCode, TimeUtils.now());
    }
}
