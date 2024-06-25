package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AfterDeleteAllDataStrategy;

@Component
public class DefaultAfterDeleteAllDataStrategy implements AfterDeleteAllDataStrategy {

    @Override
    public void apply(RefBookVersionEntity entity) {
        // Nothing to do.
    }
}
