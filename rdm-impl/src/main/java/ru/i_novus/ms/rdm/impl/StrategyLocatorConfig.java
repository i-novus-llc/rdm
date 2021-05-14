package ru.i_novus.ms.rdm.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
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
    private DefaultValidateDraftExistsStrategy defaultValidateDraftExistsStrategy;

    @Autowired
    private DefaultFindDraftEntityStrategy defaultFindDraftEntityStrategy;

    @Autowired
    private DefaultCreateDraftEntityStrategy defaultCreateDraftEntityStrategy;

    @Autowired
    private DefaultCreateDraftStorageStrategy defaultCreateDraftStorageStrategy;

    // File:
    @Autowired
    private DefaultAllowStoreVersionFileStrategy defaultAllowStoreVersionFileStrategy;

    @Autowired
    private DefaultGenerateFileNameStrategy defaultGenerateFileNameStrategy;

    @Autowired
    private DefaultFindVersionFileStrategy defaultFindVersionFileStrategy;

    @Autowired
    private DefaultCreateVersionFileStrategy defaultCreateVersionFileStrategy;

    @Autowired
    private DefaultSaveVersionFileStrategy defaultSaveVersionFileStrategy;

    @Autowired
    private DefaultExportVersionFileStrategy defaultExportVersionFileStrategy;

    @Autowired
    private DefaultSupplyPathFileContentStrategy defaultSupplyPathFileContentStrategy;

    @Autowired
    private DefaultGetExportFileStrategy defaultGetExportFileStrategy;

    /* Unversioned Strategies: */

    // RefBook:
    @Autowired
    private UnversionedCreateFirstVersionStrategy unversionedCreateFirstVersionStrategy;

    @Autowired
    private UnversionedCreateFirstStorageStrategy unversionedCreateFirstStorageStrategy;

    // Version + Draft:
    @Autowired
    private UnversionedValidateDraftExistsStrategy unversionedValidateDraftExistsStrategy;

    @Autowired
    private UnversionedFindDraftEntityStrategy unversionedFindDraftEntityStrategy;

    @Autowired
    private UnversionedCreateDraftEntityStrategy unversionedCreateDraftEntityStrategy;

    @Autowired
    private UnversionedCreateDraftStorageStrategy unversionedCreateDraftStorageStrategy;

    // File:
    @Autowired
    private UnversionedAllowStoreVersionFileStrategy unversionedAllowStoreVersionFileStrategy;

    @Autowired
    private UnversionedGenerateFileNameStrategy unversionedGenerateFileNameStrategy;

    @Autowired
    private UnversionedSaveVersionFileStrategy unversionedSaveVersionFileStrategy;

    @Autowired
    private UnversionedGetExportFileStrategy unversionedGetExportFileStrategy;

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
        result.put(ValidateDraftExistsStrategy.class, defaultValidateDraftExistsStrategy);
        result.put(FindDraftEntityStrategy.class, defaultFindDraftEntityStrategy);
        result.put(CreateDraftEntityStrategy.class, defaultCreateDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, defaultCreateDraftStorageStrategy);
        // File:
        result.put(AllowStoreVersionFileStrategy.class, defaultAllowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, defaultGenerateFileNameStrategy);
        result.put(FindVersionFileStrategy.class, defaultFindVersionFileStrategy);
        result.put(CreateVersionFileStrategy.class, defaultCreateVersionFileStrategy);
        result.put(SaveVersionFileStrategy.class, defaultSaveVersionFileStrategy);
        result.put(ExportVersionFileStrategy.class, defaultExportVersionFileStrategy);
        result.put(SupplyPathFileContentStrategy.class, defaultSupplyPathFileContentStrategy);
        result.put(GetExportFileStrategy.class, defaultGetExportFileStrategy);

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getUnversionedStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // RefBook:
        result.put(CreateFirstVersionStrategy.class, unversionedCreateFirstVersionStrategy);
        result.put(CreateFirstStorageStrategy.class, unversionedCreateFirstStorageStrategy);
        // Version + Draft:
        result.put(ValidateDraftExistsStrategy.class, unversionedValidateDraftExistsStrategy);
        result.put(FindDraftEntityStrategy.class, unversionedFindDraftEntityStrategy);
        result.put(CreateDraftEntityStrategy.class, unversionedCreateDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, unversionedCreateDraftStorageStrategy);
        // File:
        result.put(AllowStoreVersionFileStrategy.class, unversionedAllowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, unversionedGenerateFileNameStrategy);
        result.put(SaveVersionFileStrategy.class, unversionedSaveVersionFileStrategy);
        result.put(GetExportFileStrategy.class, unversionedGetExportFileStrategy);

        return result;
    }

    @Bean(name = "fileNameStrategyLocator")
    @SuppressWarnings("unused")
    public StrategyLocator fileNameStrategyLocator() {
        return new BaseStrategyLocator(getFileNameStrategiesMap());
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getFileNameStrategiesMap() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new EnumMap<>(RefBookTypeEnum.class);

        Map<Class<? extends Strategy>, Strategy> defaultStrategies = new HashMap<>();
        defaultStrategies.put(GenerateFileNameStrategy.class, defaultGenerateFileNameStrategy);
        result.put(RefBookTypeEnum.DEFAULT, defaultStrategies);

        Map<Class<? extends Strategy>, Strategy> unversionedStrategies = new HashMap<>();
        unversionedStrategies.put(GenerateFileNameStrategy.class, unversionedGenerateFileNameStrategy);
        result.put(RefBookTypeEnum.UNVERSIONED, unversionedStrategies);

        return result;
    }
}
