package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedAllowStoreVersionFileStrategy implements AllowStoreVersionFileStrategy {

    @Override
    public boolean allow(RefBookVersionEntity entity) {
        return false;
    }
}
