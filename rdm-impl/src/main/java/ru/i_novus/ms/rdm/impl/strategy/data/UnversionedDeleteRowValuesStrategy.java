package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.DeleteRowValuesStrategy;

import java.util.List;

@Component
public class UnversionedDeleteRowValuesStrategy implements DeleteRowValuesStrategy {

    @Autowired
    @Qualifier("defaultDeleteRowValuesStrategy")
    private DeleteRowValuesStrategy deleteRowValuesStrategy;

    @Override
    public void delete(RefBookVersionEntity entity, List<Object> systemIds) {

        deleteRowValuesStrategy.delete(entity, systemIds);
    }
}
