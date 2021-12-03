package ru.i_novus.ms.rdm.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.data.*;
import ru.i_novus.ms.rdm.impl.strategy.draft.*;
import ru.i_novus.ms.rdm.impl.strategy.file.*;
import ru.i_novus.ms.rdm.impl.strategy.publish.*;
import ru.i_novus.ms.rdm.impl.strategy.refbook.*;
import ru.i_novus.ms.rdm.impl.strategy.structure.*;
import ru.i_novus.ms.rdm.impl.strategy.version.DefaultValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration @Lazy
public class StrategyLocatorConfig {

    /* Default Strategies: */

    // RefBook:
    @Autowired @Lazy
    private DefaultRefBookCreateValidationStrategy defaultRefBookCreateValidationStrategy;

    @Autowired @Lazy
    private DefaultCreateRefBookEntityStrategy defaultCreateRefBookEntityStrategy;
    @Autowired @Lazy
    private DefaultCreateFirstVersionStrategy defaultCreateFirstVersionStrategy;
    @Autowired @Lazy
    private DefaultCreateFirstStorageStrategy defaultCreateFirstStorageStrategy;

    // Version + Draft:
    @Autowired @Lazy
    private DefaultValidateVersionNotArchivedStrategy defaultValidateVersionNotArchivedStrategy;

    @Autowired @Lazy
    private DefaultFindDraftEntityStrategy defaultFindDraftEntityStrategy;
    @Autowired @Lazy
    private DefaultCreateDraftEntityStrategy defaultCreateDraftEntityStrategy;
    @Autowired @Lazy
    private DefaultCreateDraftStorageStrategy defaultCreateDraftStorageStrategy;

    // Publish:
    @Autowired @Lazy
    private DefaultBasePublishStrategy defaultBasePublishStrategy;
    @Autowired @Lazy
    private DefaultAfterPublishStrategy defaultAfterPublishStrategy;

    // Data:
    @Autowired @Lazy
    private DefaultAddRowValuesStrategy defaultAddRowValuesStrategy;
    @Autowired @Lazy
    private DefaultUpdateRowValuesStrategy defaultUpdateRowValuesStrategy;
    @Autowired @Lazy
    private DefaultDeleteRowValuesStrategy defaultDeleteRowValuesStrategy;
    @Autowired @Lazy
    private DefaultDeleteAllRowValuesStrategy defaultDeleteAllRowValuesStrategy;
    @Autowired @Lazy
    private DefaultAfterUploadDataStrategy defaultAfterUploadDataStrategy;

    // Structure:
    @Autowired @Lazy
    private DefaultCreateAttributeStrategy defaultCreateAttributeStrategy;
    @Autowired @Lazy
    private DefaultUpdateAttributeStrategy defaultUpdateAttributeStrategy;
    @Autowired @Lazy
    private DefaultDeleteAttributeStrategy defaultDeleteAttributeStrategy;

    // File:
    @Autowired @Lazy
    private DefaultAllowStoreVersionFileStrategy defaultAllowStoreVersionFileStrategy;
    @Autowired @Lazy
    private DefaultGenerateFileNameStrategy defaultGenerateFileNameStrategy;

    /* Unversioned Strategies: */

    // RefBook:
    @Autowired @Lazy
    private UnversionedCreateRefBookEntityStrategy unversionedCreateRefBookEntityStrategy;
    @Autowired @Lazy
    private UnversionedCreateFirstStorageStrategy unversionedCreateFirstStorageStrategy;

    // Version + Draft:
    @Autowired @Lazy
    private UnversionedFindDraftEntityStrategy unversionedFindDraftEntityStrategy;
    @Autowired @Lazy
    private UnversionedCreateDraftStorageStrategy unversionedCreateDraftStorageStrategy;

    // Publish:
    @Autowired @Lazy
    private UnversionedBasePublishStrategy unversionedBasePublishStrategy;

    // Data:
    @Autowired @Lazy
    private UnversionedAddRowValuesStrategy unversionedAddRowValuesStrategy;
    @Autowired @Lazy
    private UnversionedUpdateRowValuesStrategy unversionedUpdateRowValuesStrategy;
    @Autowired @Lazy
    private UnversionedDeleteRowValuesStrategy unversionedDeleteRowValuesStrategy;
    @Autowired @Lazy
    private UnversionedDeleteAllRowValuesStrategy unversionedDeleteAllRowValuesStrategy;
    @Autowired @Lazy
    private UnversionedAfterUploadDataStrategy unversionedAfterUploadDataStrategy;

    // Structure:
    @Autowired @Lazy
    private UnversionedCreateAttributeStrategy unversionedCreateAttributeStrategy;
    @Autowired @Lazy
    private UnversionedUpdateAttributeStrategy unversionedUpdateAttributeStrategy;
    @Autowired @Lazy
    private UnversionedDeleteAttributeStrategy unversionedDeleteAttributeStrategy;

    // File:
    @Autowired @Lazy
    private UnversionedAllowStoreVersionFileStrategy unversionedAllowStoreVersionFileStrategy;
    @Autowired @Lazy
    private UnversionedGenerateFileNameStrategy unversionedGenerateFileNameStrategy;

    @Bean @Lazy
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

        // Publish:
        result.put(BasePublishStrategy.class, defaultBasePublishStrategy);
        result.put(AfterPublishStrategy.class, defaultAfterPublishStrategy);

        // Data:
        result.put(AddRowValuesStrategy.class, defaultAddRowValuesStrategy);
        result.put(UpdateRowValuesStrategy.class, defaultUpdateRowValuesStrategy);
        result.put(DeleteRowValuesStrategy.class, defaultDeleteRowValuesStrategy);
        result.put(DeleteAllRowValuesStrategy.class, defaultDeleteAllRowValuesStrategy);
        result.put(AfterUploadDataStrategy.class, defaultAfterUploadDataStrategy);

        // Structure:
        result.put(CreateAttributeStrategy.class, defaultCreateAttributeStrategy);
        result.put(UpdateAttributeStrategy.class, defaultUpdateAttributeStrategy);
        result.put(DeleteAttributeStrategy.class, defaultDeleteAttributeStrategy);

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

        // Publish:
        result.put(BasePublishStrategy.class, unversionedBasePublishStrategy);

        // Data:
        result.put(AddRowValuesStrategy.class, unversionedAddRowValuesStrategy);
        result.put(UpdateRowValuesStrategy.class, unversionedUpdateRowValuesStrategy);
        result.put(DeleteRowValuesStrategy.class, unversionedDeleteRowValuesStrategy);
        result.put(DeleteAllRowValuesStrategy.class, unversionedDeleteAllRowValuesStrategy);
        result.put(AfterUploadDataStrategy.class, unversionedAfterUploadDataStrategy);

        // Structure:
        result.put(CreateAttributeStrategy.class, unversionedCreateAttributeStrategy);
        result.put(UpdateAttributeStrategy.class, unversionedUpdateAttributeStrategy);
        result.put(DeleteAttributeStrategy.class, unversionedDeleteAttributeStrategy);

        // File:
        result.put(AllowStoreVersionFileStrategy.class, unversionedAllowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, unversionedGenerateFileNameStrategy);

        return result;
    }
}
