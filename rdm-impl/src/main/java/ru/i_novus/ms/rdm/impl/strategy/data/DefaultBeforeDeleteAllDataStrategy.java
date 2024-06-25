package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.BeforeDeleteAllDataStrategy;

@Component
public class DefaultBeforeDeleteAllDataStrategy implements BeforeDeleteAllDataStrategy {

    @Override
    public void apply(RefBookVersionEntity entity) {
        // Nothing to do.
    }
}
