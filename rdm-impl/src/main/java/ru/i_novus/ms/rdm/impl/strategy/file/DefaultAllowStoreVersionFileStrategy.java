package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class DefaultAllowStoreVersionFileStrategy implements AllowStoreVersionFileStrategy {

    @Override
    public boolean allow(RefBookVersionEntity entity) {
        return entity != null && !entity.isDraft();
    }
}
