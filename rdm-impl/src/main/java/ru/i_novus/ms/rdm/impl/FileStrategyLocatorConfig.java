package ru.i_novus.ms.rdm.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.file.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FileStrategyLocatorConfig {

    /* Default Strategies: */

    // File:
    @Autowired
    private DefaultAllowStoreVersionFileStrategy defaultAllowStoreVersionFileStrategy;
    @Autowired
    private DefaultGenerateFileNameStrategy defaultGenerateFileNameStrategy;

    /* Unversioned Strategies: */

    // File:
    @Autowired
    private UnversionedAllowStoreVersionFileStrategy unversionedAllowStoreVersionFileStrategy;
    @Autowired
    private UnversionedGenerateFileNameStrategy unversionedGenerateFileNameStrategy;

    @Bean
    @SuppressWarnings("unused")
    public StrategyLocator fileStrategyLocator() {
        return new BaseStrategyLocator(getStrategiesMap());
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategiesMap() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new EnumMap<>(RefBookTypeEnum.class);
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());
        result.put(RefBookTypeEnum.UNVERSIONED, getUnversionedStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();

        // File:
        result.put(AllowStoreVersionFileStrategy.class, defaultAllowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, defaultGenerateFileNameStrategy);

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getUnversionedStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();

        // File:
        result.put(AllowStoreVersionFileStrategy.class, unversionedAllowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, unversionedGenerateFileNameStrategy);

        return result;
    }
}
