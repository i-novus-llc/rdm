package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AfterDeleteDataStrategy;

import java.util.List;

@Component
public class DefaultAfterDeleteDataStrategy implements AfterDeleteDataStrategy {

    @Override
    public void apply(RefBookVersionEntity entity, List<Object> systemIds) {
        // Nothing to do.
    }
}
