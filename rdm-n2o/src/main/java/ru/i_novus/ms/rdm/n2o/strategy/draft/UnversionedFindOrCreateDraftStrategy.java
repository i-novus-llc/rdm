package ru.i_novus.ms.rdm.n2o.strategy.draft;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;

@Component
public class UnversionedFindOrCreateDraftStrategy implements FindOrCreateDraftStrategy {

    @Override
    public UiDraft findOrCreate(RefBookVersion version) {

        return new UiDraft(version);
    }
}
