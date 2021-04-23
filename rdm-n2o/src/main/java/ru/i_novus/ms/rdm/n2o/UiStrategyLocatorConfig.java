package ru.i_novus.ms.rdm.n2o;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.n2o.strategy.BaseUiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.draft.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class UiStrategyLocatorConfig {

    @Autowired
    private DefaultFindOrCreateDraftStrategy defaultFindOrCreateDraftStrategy;

    @Autowired
    private DefaultValidateIsDraftStrategy defaultValidateIsDraftStrategy;

    @Autowired
    private UnversionedFindOrCreateDraftStrategy unversionedFindOrCreateDraftStrategy;

    @Autowired
    private UnversionedValidateIsDraftStrategy unversionedValidateIsDraftStrategy;

    @Bean
    public UiStrategyLocator strategyLocator() {
        return new BaseUiStrategyLocator(getStrategiesMap());
    }

    private Map<RefBookType, Map<Class<? extends UiStrategy>, UiStrategy>> getStrategiesMap() {

        Map<RefBookType, Map<Class<? extends UiStrategy>, UiStrategy>> result = new EnumMap<>(RefBookType.class);
        result.put(RefBookType.DEFAULT, getDefaultStrategies());
        result.put(RefBookType.UNVERSIONED, getUnversionedStrategies());

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getDefaultStrategies() {

        Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        // RefBook:
        // Draft:
        result.put(FindOrCreateDraftStrategy.class, defaultFindOrCreateDraftStrategy);
        result.put(ValidateIsDraftStrategy.class, defaultValidateIsDraftStrategy);

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getUnversionedStrategies() {

        Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        // RefBook:
        // Draft:
        result.put(FindOrCreateDraftStrategy.class, unversionedFindOrCreateDraftStrategy);
        result.put(ValidateIsDraftStrategy.class, unversionedValidateIsDraftStrategy);

        return result;
    }
}
