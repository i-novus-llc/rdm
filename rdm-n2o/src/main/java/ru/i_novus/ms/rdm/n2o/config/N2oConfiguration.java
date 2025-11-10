package ru.i_novus.ms.rdm.n2o.config;

import net.n2oapp.cache.template.SyncCacheTemplate;
import net.n2oapp.framework.api.MetadataEnvironment;
import net.n2oapp.framework.api.data.CriteriaConstructor;
import net.n2oapp.framework.api.data.QueryExceptionHandler;
import net.n2oapp.framework.api.data.QueryProcessor;
import net.n2oapp.framework.api.register.MetadataRegister;
import net.n2oapp.framework.config.compile.pipeline.operation.CompileCacheOperation;
import net.n2oapp.framework.config.compile.pipeline.operation.SourceCacheOperation;
import net.n2oapp.framework.engine.data.N2oInvocationFactory;
import net.n2oapp.framework.engine.data.N2oQueryProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.i_novus.ms.rdm.n2o.criteria.construct.RdmCriteriaConstructorFactory;
import ru.i_novus.ms.rdm.n2o.operation.RdmCompileCacheOperation;
import ru.i_novus.ms.rdm.n2o.operation.RdmSourceCacheOperation;

import java.util.List;

/**
 * Минимально необходимая конфигурация N2O для RDM.
 */
@Configuration
@SuppressWarnings({"rawtypes","java:S3740"})
public class N2oConfiguration {

    @Bean
    public RdmCriteriaConstructorFactory rdmCriteriaConstructorFactory(
            List<CriteriaConstructor<?>> criteriaConstructors
    ) {
        return new RdmCriteriaConstructorFactory(criteriaConstructors);
    }

    @Bean
    public QueryProcessor rdmQueryProcessor(
            N2oInvocationFactory invocationFactory,
            QueryExceptionHandler exceptionHandler,
            MetadataEnvironment metadataEnvironment,
            RdmCriteriaConstructorFactory rdmCriteriaConstructorFactory
    ) {
        final N2oQueryProcessor queryProcessor = new N2oQueryProcessor(invocationFactory, exceptionHandler);
        queryProcessor.setEnvironment(metadataEnvironment);
        queryProcessor.setCriteriaConstructorFactory(rdmCriteriaConstructorFactory);
        return queryProcessor;
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryProcessor queryProcessor(
            N2oInvocationFactory invocationFactory,
            QueryExceptionHandler exceptionHandler,
            MetadataEnvironment metadataEnvironment,
            RdmCriteriaConstructorFactory rdmCriteriaConstructorFactory
    ) {
        return rdmQueryProcessor(invocationFactory, exceptionHandler, metadataEnvironment, rdmCriteriaConstructorFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public CompileCacheOperation compileCacheOperation(CacheManager cacheManager) {
        return new RdmCompileCacheOperation(new SyncCacheTemplate(cacheManager));
    }

    @Bean
    @ConditionalOnMissingBean
    public SourceCacheOperation sourceCacheOperation(CacheManager cacheManager, MetadataRegister metadataRegister) {
        return new RdmSourceCacheOperation(new SyncCacheTemplate(cacheManager), metadataRegister);
    }
}

