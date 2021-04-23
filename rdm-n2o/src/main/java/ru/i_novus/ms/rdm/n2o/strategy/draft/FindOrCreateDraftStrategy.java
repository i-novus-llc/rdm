package ru.i_novus.ms.rdm.n2o.strategy.draft;

import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;

public interface FindOrCreateDraftStrategy extends UiStrategy {

    UiDraft findOrCreate(RefBookVersion version);
}
