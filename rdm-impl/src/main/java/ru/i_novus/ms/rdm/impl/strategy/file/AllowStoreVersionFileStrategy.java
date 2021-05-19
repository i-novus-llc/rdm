package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface AllowStoreVersionFileStrategy extends Strategy {

    boolean allow(RefBookVersion version);
}
