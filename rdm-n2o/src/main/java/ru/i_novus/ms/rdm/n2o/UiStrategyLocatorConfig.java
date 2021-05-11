package ru.i_novus.ms.rdm.n2o;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.n2o.strategy.BaseUiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.draft.*;
import ru.i_novus.ms.rdm.n2o.strategy.version.DefaultGetDisplayNumberStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.version.GetDisplayNumberStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.version.UnversionedGetDisplayNumberStrategy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class UiStrategyLocatorConfig {

    /* Default Strategies: */

    // Version + Draft:
    @Autowired
    private DefaultGetDisplayNumberStrategy defaultGetDisplayNumberStrategy;

    @Autowired
    private DefaultFindOrCreateDraftStrategy defaultFindOrCreateDraftStrategy;

    @Autowired
    private DefaultValidateIsDraftStrategy defaultValidateIsDraftStrategy;

    /* Unversioned Strategies: */

    // Version + Draft:
    @Autowired
    private UnversionedGetDisplayNumberStrategy unversionedGetDisplayNumberStrategy;

    @Autowired
    private UnversionedFindOrCreateDraftStrategy unversionedFindOrCreateDraftStrategy;

    @Autowired
    private UnversionedValidateIsDraftStrategy unversionedValidateIsDraftStrategy;

    @Bean
    public UiStrategyLocator strategyLocator() {
        return new BaseUiStrategyLocator(getStrategiesMap());
    }

    private Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> getStrategiesMap() {

        Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> result = new EnumMap<>(RefBookTypeEnum.class);
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());
        result.put(RefBookTypeEnum.UNVERSIONED, getUnversionedStrategies());

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getDefaultStrategies() {

        Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        // Version + Draft:
        result.put(GetDisplayNumberStrategy.class, defaultGetDisplayNumberStrategy);
        result.put(FindOrCreateDraftStrategy.class, defaultFindOrCreateDraftStrategy);
        result.put(ValidateIsDraftStrategy.class, defaultValidateIsDraftStrategy);

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getUnversionedStrategies() {

        Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        // Version + Draft:
        result.put(GetDisplayNumberStrategy.class, unversionedGetDisplayNumberStrategy);
        result.put(FindOrCreateDraftStrategy.class, unversionedFindOrCreateDraftStrategy);
        result.put(ValidateIsDraftStrategy.class, unversionedValidateIsDraftStrategy);

        return result;
    }
}
