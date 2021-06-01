package ru.i_novus.ms.rdm.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.data.*;
import ru.i_novus.ms.rdm.impl.strategy.draft.*;
import ru.i_novus.ms.rdm.impl.strategy.file.*;
import ru.i_novus.ms.rdm.impl.strategy.refbook.*;
import ru.i_novus.ms.rdm.impl.strategy.version.DefaultValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class StrategyLocatorConfig {

    /* Default Strategies: */

    // RefBook:
    @Autowired
    private DefaultRefBookCreateValidationStrategy defaultRefBookCreateValidationStrategy;

    @Autowired
    private DefaultCreateRefBookEntityStrategy defaultCreateRefBookEntityStrategy;

    @Autowired
    private DefaultCreateFirstVersionStrategy defaultCreateFirstVersionStrategy;

    @Autowired
    private DefaultCreateFirstStorageStrategy defaultCreateFirstStorageStrategy;

    // Version + Draft:
    @Autowired
    private DefaultValidateVersionNotArchivedStrategy defaultValidateVersionNotArchivedStrategy;

    @Autowired
    private DefaultFindDraftEntityStrategy defaultFindDraftEntityStrategy;

    @Autowired
    private DefaultCreateDraftEntityStrategy defaultCreateDraftEntityStrategy;

    @Autowired
    private DefaultCreateDraftStorageStrategy defaultCreateDraftStorageStrategy;

    // Data:
    @Autowired
    private DefaultAddRowValuesStrategy defaultAddRowValuesStrategy;
    @Autowired
    private DefaultUpdateRowValuesStrategy defaultUpdateRowValuesStrategy;
    @Autowired
    private DefaultDeleteRowValuesStrategy defaultDeleteRowValuesStrategy;
    @Autowired
    private DefaultDeleteAllRowValuesStrategy defaultDeleteAllRowValuesStrategy;

    // File:
    @Autowired
    private DefaultAllowStoreVersionFileStrategy defaultAllowStoreVersionFileStrategy;

    @Autowired
    private DefaultGenerateFileNameStrategy defaultGenerateFileNameStrategy;

    /* Unversioned Strategies: */

    // RefBook:
    @Autowired
    private UnversionedCreateRefBookEntityStrategy unversionedCreateRefBookEntityStrategy;

    @Autowired
    private UnversionedCreateFirstStorageStrategy unversionedCreateFirstStorageStrategy;

    // Version + Draft:
    @Autowired
    private UnversionedFindDraftEntityStrategy unversionedFindDraftEntityStrategy;

    // Data:
    @Autowired
    private UnversionedAddRowValuesStrategy unversionedAddRowValuesStrategy;
    @Autowired
    private UnversionedDeleteRowValuesStrategy unversionedDeleteRowValuesStrategy;

    @Autowired
    private UnversionedCreateDraftStorageStrategy unversionedCreateDraftStorageStrategy;

    // File:
    @Autowired
    private UnversionedAllowStoreVersionFileStrategy unversionedAllowStoreVersionFileStrategy;

    @Autowired
    private UnversionedGenerateFileNameStrategy unversionedGenerateFileNameStrategy;

    @Bean
    @SuppressWarnings("unused")
    public StrategyLocator strategyLocator() {
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
        // RefBook:
        result.put(RefBookCreateValidationStrategy.class, defaultRefBookCreateValidationStrategy);
        result.put(CreateRefBookEntityStrategy.class, defaultCreateRefBookEntityStrategy);
        result.put(CreateFirstVersionStrategy.class, defaultCreateFirstVersionStrategy);
        result.put(CreateFirstStorageStrategy.class, defaultCreateFirstStorageStrategy);
        // Version + Draft:
        result.put(ValidateVersionNotArchivedStrategy.class, defaultValidateVersionNotArchivedStrategy);
        result.put(FindDraftEntityStrategy.class, defaultFindDraftEntityStrategy);
        result.put(CreateDraftEntityStrategy.class, defaultCreateDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, defaultCreateDraftStorageStrategy);
        // Data:
        result.put(AddRowValuesStrategy.class, defaultAddRowValuesStrategy);
        result.put(UpdateRowValuesStrategy.class, defaultUpdateRowValuesStrategy);
        result.put(DeleteRowValuesStrategy.class, defaultDeleteRowValuesStrategy);
        result.put(DeleteAllRowValuesStrategy.class, defaultDeleteAllRowValuesStrategy);
        // File:
        result.put(AllowStoreVersionFileStrategy.class, defaultAllowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, defaultGenerateFileNameStrategy);

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getUnversionedStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // RefBook:
        result.put(CreateRefBookEntityStrategy.class, unversionedCreateRefBookEntityStrategy);
        result.put(CreateFirstStorageStrategy.class, unversionedCreateFirstStorageStrategy);
        // Version + Draft:
        result.put(FindDraftEntityStrategy.class, unversionedFindDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, unversionedCreateDraftStorageStrategy);
        // Data:
        result.put(AddRowValuesStrategy.class, unversionedAddRowValuesStrategy);
        result.put(DeleteRowValuesStrategy.class, unversionedDeleteRowValuesStrategy);
        // File:
        result.put(AllowStoreVersionFileStrategy.class, unversionedAllowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, unversionedGenerateFileNameStrategy);

        return result;
    }
}
