package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.BeforeDeleteDataStrategy;
import ru.i_novus.ms.rdm.impl.strategy.referrer.UnversionedBeforeDeleteProcessReferrersStrategy;

import java.util.List;

@Component
public class UnversionedBeforeDeleteDataStrategy implements BeforeDeleteDataStrategy {

    @Autowired
    private UnversionedBeforeDeleteProcessReferrersStrategy processReferrersStrategy;

    @Override
    public void apply(RefBookVersionEntity entity, List<Object> systemIds) {

        processReferrersStrategy.apply(entity, systemIds);
    }
}
