package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

@Component
public class UnversionedAllowStoreVersionFileStrategy implements AllowStoreVersionFileStrategy {

    @Override
    public boolean allow(RefBookVersion version) {
        return false;
    }
}
