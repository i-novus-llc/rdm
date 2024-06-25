package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.BeforeDeleteAllDataStrategy;
import ru.i_novus.ms.rdm.impl.strategy.referrer.UnversionedBeforeDeleteAllProcessReferrersStrategy;

@Component
public class UnversionedBeforeDeleteAllDataStrategy implements BeforeDeleteAllDataStrategy {

    @Autowired
    private UnversionedBeforeDeleteAllProcessReferrersStrategy processReferrersStrategy;

    @Override
    public void apply(RefBookVersionEntity entity) {

        processReferrersStrategy.apply(entity);
    }
}
