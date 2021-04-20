package ru.i_novus.ms.rdm.n2o.strategy.draft;

import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;

public interface ValidateIsDraftStrategy extends UiStrategy {

    void validate(RefBookVersion version);
}
