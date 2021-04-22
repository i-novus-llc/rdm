package ru.i_novus.ms.rdm.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.draft.DefaultValidateDraftExistsStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.UnversionedValidateDraftExistsStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.ValidateDraftExistsStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.*;
import ru.i_novus.ms.rdm.impl.strategy.version.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class StrategyLocatorConfig {

    @Autowired
    private DefaultRefBookCreateValidationStrategy defaultRefBookCreateValidationStrategy;

    @Autowired
    private DefaultCreateRefBookEntityStrategy defaultCreateRefBookEntityStrategy;

    @Autowired
    private DefaultCreateFirstVersionStrategy defaultCreateFirstVersionStrategy;

    @Autowired
    private DefaultCreateFirstStorageStrategy defaultCreateFirstStorageStrategy;

    @Autowired
    private DefaultValidateVersionExistsStrategy defaultValidateVersionExistsStrategy;

    @Autowired
    private DefaultValidateVersionNotArchivedStrategy defaultValidateVersionNotArchivedStrategy;

    @Autowired
    private DefaultValidateDraftExistsStrategy defaultValidateDraftExistsStrategy;

    @Autowired
    private UnversionedCreateFirstVersionStrategy unversionedCreateFirstVersionStrategy;

    @Autowired
    private UnversionedCreateFirstStorageStrategy unversionedCreateFirstStorageStrategy;

    @Autowired
    private UnversionedValidateDraftExistsStrategy unversionedValidateDraftExistsStrategy;

    @Bean
    public StrategyLocator strategyLocator() {
        return new BaseStrategyLocator(getStrategiesMap());
    }

    private Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> getStrategiesMap() {

        Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> result = new EnumMap<>(RefBookType.class);
        result.put(RefBookType.DEFAULT, getDefaultStrategies());
        result.put(RefBookType.UNVERSIONED, getUnversionedStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // RefBook:
        result.put(RefBookCreateValidationStrategy.class, defaultRefBookCreateValidationStrategy);
        result.put(CreateRefBookEntityStrategy.class, defaultCreateRefBookEntityStrategy);
        result.put(CreateFirstVersionStrategy.class, defaultCreateFirstVersionStrategy);
        result.put(CreateFirstStorageStrategy.class, defaultCreateFirstStorageStrategy);
        // Version + Draft:
        result.put(ValidateVersionExistsStrategy.class, defaultValidateVersionExistsStrategy);
        result.put(ValidateVersionNotArchivedStrategy.class, defaultValidateVersionNotArchivedStrategy);
        result.put(ValidateDraftExistsStrategy.class, defaultValidateDraftExistsStrategy);

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getUnversionedStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // RefBook:
        result.put(CreateFirstVersionStrategy.class, unversionedCreateFirstVersionStrategy);
        result.put(CreateFirstStorageStrategy.class, unversionedCreateFirstStorageStrategy);
        // Version + Draft:
        result.put(ValidateDraftExistsStrategy.class, unversionedValidateDraftExistsStrategy);

        return result;
    }
}
