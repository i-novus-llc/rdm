package ru.i_novus.ms.rdm.impl.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.data.*;
import ru.i_novus.ms.rdm.impl.strategy.data.api.*;
import ru.i_novus.ms.rdm.impl.strategy.draft.*;
import ru.i_novus.ms.rdm.impl.strategy.publish.*;
import ru.i_novus.ms.rdm.impl.strategy.refbook.*;
import ru.i_novus.ms.rdm.impl.strategy.structure.*;
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

    // Publish:
    @Autowired
    private DefaultBasePublishStrategy defaultBasePublishStrategy;
    @Autowired
    private DefaultPublishEndStrategy defaultAfterPublishStrategy;
    @Autowired
    private DefaultEditPublishStrategy defaultEditPublishStrategy;

    // Data:
    @Autowired
    private DefaultAddRowValuesStrategy defaultAddRowValuesStrategy;
    @Autowired
    private DefaultUpdateRowValuesStrategy defaultUpdateRowValuesStrategy;
    @Autowired
    private DefaultDeleteRowValuesStrategy defaultDeleteRowValuesStrategy;
    @Autowired
    private DefaultDeleteAllRowValuesStrategy defaultDeleteAllRowValuesStrategy;
    @Autowired
    private DefaultAfterUploadDataStrategy defaultAfterUploadDataStrategy;
    @Autowired
    private DefaultAfterUpdateDataStrategy defaultAfterUpdateDataStrategy;
    @Autowired
    private DefaultBeforeDeleteDataStrategy defaultBeforeDeleteDataStrategy;
    @Autowired
    private DefaultAfterDeleteDataStrategy defaultAfterDeleteDataStrategy;
    @Autowired
    private DefaultBeforeDeleteAllDataStrategy defaultBeforeDeleteAllDataStrategy;
    @Autowired
    private DefaultAfterDeleteAllDataStrategy defaultAfterDeleteAllDataStrategy;

    // Structure:
    @Autowired
    private DefaultCreateAttributeStrategy defaultCreateAttributeStrategy;
    @Autowired
    private DefaultUpdateAttributeStrategy defaultUpdateAttributeStrategy;
    @Autowired
    private DefaultDeleteAttributeStrategy defaultDeleteAttributeStrategy;

    /* Unversioned Strategies: */

    // RefBook:
    @Autowired
    private UnversionedCreateRefBookEntityStrategy unversionedCreateRefBookEntityStrategy;
    @Autowired
    private UnversionedCreateFirstStorageStrategy unversionedCreateFirstStorageStrategy;

    // Version + Draft:
    @Autowired
    private UnversionedFindDraftEntityStrategy unversionedFindDraftEntityStrategy;
    @Autowired
    private UnversionedCreateDraftStorageStrategy unversionedCreateDraftStorageStrategy;

    // Publish:
    @Autowired
    private UnversionedBasePublishStrategy unversionedBasePublishStrategy;
    @Autowired
    private UnversionedEditPublishStrategy unversionedEditPublishStrategy;

    // Data:
    @Autowired
    private UnversionedAddRowValuesStrategy unversionedAddRowValuesStrategy;
    @Autowired
    private UnversionedUpdateRowValuesStrategy unversionedUpdateRowValuesStrategy;
    @Autowired
    private UnversionedDeleteRowValuesStrategy unversionedDeleteRowValuesStrategy;
    @Autowired
    private UnversionedDeleteAllRowValuesStrategy unversionedDeleteAllRowValuesStrategy;
    @Autowired
    private UnversionedAfterUploadDataStrategy unversionedAfterUploadDataStrategy;
    @Autowired
    private UnversionedAfterUpdateDataStrategy unversionedAfterUpdateDataStrategy;
    @Autowired
    private UnversionedBeforeDeleteDataStrategy unversionedBeforeDeleteDataStrategy;
    @Autowired
    private UnversionedAfterDeleteDataStrategy unversionedAfterDeleteDataStrategy;
    @Autowired
    private UnversionedBeforeDeleteAllDataStrategy unversionedBeforeDeleteAllDataStrategy;
    @Autowired
    private UnversionedAfterDeleteAllDataStrategy unversionedAfterDeleteAllDataStrategy;

    // Structure:
    @Autowired
    private UnversionedCreateAttributeStrategy unversionedCreateAttributeStrategy;
    @Autowired
    private UnversionedUpdateAttributeStrategy unversionedUpdateAttributeStrategy;
    @Autowired
    private UnversionedDeleteAttributeStrategy unversionedDeleteAttributeStrategy;

    @Bean
    @SuppressWarnings("unused")
    public StrategyLocator strategyLocator() {
        return new BaseStrategyLocator(getStrategiesMap());
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategiesMap() {

        final Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new EnumMap<>(RefBookTypeEnum.class);
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());
        result.put(RefBookTypeEnum.UNVERSIONED, getUnversionedStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        final Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();

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
        result.put(PublishEndStrategy.class, defaultAfterPublishStrategy);
        result.put(EditPublishStrategy.class, defaultEditPublishStrategy);

        // Data:
        result.put(AddRowValuesStrategy.class, defaultAddRowValuesStrategy);
        result.put(UpdateRowValuesStrategy.class, defaultUpdateRowValuesStrategy);
        result.put(DeleteRowValuesStrategy.class, defaultDeleteRowValuesStrategy);
        result.put(DeleteAllRowValuesStrategy.class, defaultDeleteAllRowValuesStrategy);
        result.put(AfterUploadDataStrategy.class, defaultAfterUploadDataStrategy);

        result.put(AfterUpdateDataStrategy.class, defaultAfterUpdateDataStrategy);
        result.put(BeforeDeleteDataStrategy.class, defaultBeforeDeleteDataStrategy);
        result.put(AfterDeleteDataStrategy.class, defaultAfterDeleteDataStrategy);
        result.put(BeforeDeleteAllDataStrategy.class, defaultBeforeDeleteAllDataStrategy);
        result.put(AfterDeleteAllDataStrategy.class, defaultAfterDeleteAllDataStrategy);

        // Structure:
        result.put(CreateAttributeStrategy.class, defaultCreateAttributeStrategy);
        result.put(UpdateAttributeStrategy.class, defaultUpdateAttributeStrategy);
        result.put(DeleteAttributeStrategy.class, defaultDeleteAttributeStrategy);

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getUnversionedStrategies() {

        final Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();

        // RefBook:
        result.put(CreateRefBookEntityStrategy.class, unversionedCreateRefBookEntityStrategy);
        result.put(CreateFirstStorageStrategy.class, unversionedCreateFirstStorageStrategy);

        // Version + Draft:
        result.put(FindDraftEntityStrategy.class, unversionedFindDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, unversionedCreateDraftStorageStrategy);

        // Publish:
        result.put(BasePublishStrategy.class, unversionedBasePublishStrategy);
        result.put(EditPublishStrategy.class, unversionedEditPublishStrategy);

        // Data:
        result.put(AddRowValuesStrategy.class, unversionedAddRowValuesStrategy);
        result.put(UpdateRowValuesStrategy.class, unversionedUpdateRowValuesStrategy);
        result.put(DeleteRowValuesStrategy.class, unversionedDeleteRowValuesStrategy);
        result.put(DeleteAllRowValuesStrategy.class, unversionedDeleteAllRowValuesStrategy);
        result.put(AfterUploadDataStrategy.class, unversionedAfterUploadDataStrategy);

        result.put(AfterUpdateDataStrategy.class, unversionedAfterUpdateDataStrategy);
        result.put(BeforeDeleteDataStrategy.class, unversionedBeforeDeleteDataStrategy);
        result.put(AfterDeleteDataStrategy.class, unversionedAfterDeleteDataStrategy);
        result.put(BeforeDeleteAllDataStrategy.class, unversionedBeforeDeleteAllDataStrategy);
        result.put(AfterDeleteAllDataStrategy.class, unversionedAfterDeleteAllDataStrategy);

        // Structure:
        result.put(CreateAttributeStrategy.class, unversionedCreateAttributeStrategy);
        result.put(UpdateAttributeStrategy.class, unversionedUpdateAttributeStrategy);
        result.put(DeleteAttributeStrategy.class, unversionedDeleteAttributeStrategy);

        return result;
    }
}
