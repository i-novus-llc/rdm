package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.BeforeDeleteDataStrategy;

import java.util.List;

@Component
public class DefaultBeforeDeleteDataStrategy implements BeforeDeleteDataStrategy {

    @Override
    public void apply(RefBookVersionEntity entity, List<Object> systemIds) {
        // Nothing to do.
    }
}
