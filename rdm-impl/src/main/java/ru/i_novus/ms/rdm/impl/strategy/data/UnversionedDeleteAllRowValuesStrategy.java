package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.DeleteAllRowValuesStrategy;

@Component
public class UnversionedDeleteAllRowValuesStrategy implements DeleteAllRowValuesStrategy {

    @Autowired
    @Qualifier("defaultDeleteAllRowValuesStrategy")
    private DeleteAllRowValuesStrategy deleteAllRowValuesStrategy;

    @Override
    public void deleteAll(RefBookVersionEntity entity) {

        deleteAllRowValuesStrategy.deleteAll(entity);
    }
}
