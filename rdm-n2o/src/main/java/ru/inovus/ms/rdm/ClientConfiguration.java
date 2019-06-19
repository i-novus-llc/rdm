package ru.inovus.ms.rdm;

import net.n2oapp.cache.template.SyncCacheTemplate;
import net.n2oapp.framework.api.context.ContextProcessor;
import net.n2oapp.framework.api.data.DomainProcessor;
import net.n2oapp.framework.api.data.QueryExceptionHandler;
import net.n2oapp.framework.api.data.QueryProcessor;
import net.n2oapp.framework.api.register.MetadataRegister;
import net.n2oapp.framework.config.compile.pipeline.operation.CompileCacheOperation;
import net.n2oapp.framework.config.compile.pipeline.operation.SourceCacheOperation;
import net.n2oapp.framework.engine.data.N2oInvocationFactory;
import net.n2oapp.framework.engine.data.N2oQueryProcessor;
import net.n2oapp.platform.jaxrs.LocalDateTimeISOParameterConverter;
import net.n2oapp.platform.jaxrs.TypedParamConverter;
import org.apache.cxf.jaxrs.provider.JavaTimeTypesParamConverterProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.criteria.RestCriteriaConstructor;
import ru.inovus.ms.rdm.operation.RdmCompileCacheOperation;
import ru.inovus.ms.rdm.operation.RdmSourceCacheOperation;
import ru.inovus.ms.rdm.provider.*;
import ru.inovus.ms.rdm.util.json.RdmN2oLocalDateTimeMapperPreparer;

import java.time.LocalDateTime;

@Configuration
public class ClientConfiguration {

    @Bean
    public AttributeFilterConverter attributeFilterConverter() {
        return new AttributeFilterConverter();
    }

    @Bean
    public OffsetDateTimeParamConverter offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    RdmN2oLocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new RdmN2oLocalDateTimeMapperPreparer();
    }

    @Bean
    ExportFileProvider exportFileProvider(){
        return new ExportFileProvider();
    }

    @Bean
    RdmMapperConfigurer rdmMapperConfigurer(){
        return new RdmMapperConfigurer();
    }

    @Bean
    public QueryProcessor queryProcessor(ContextProcessor contextProcessor,
                                         N2oInvocationFactory invocationFactory,
                                         DomainProcessor domainProcessor,
                                         QueryExceptionHandler exceptionHandler) {
        N2oQueryProcessor queryProcessor = new N2oQueryProcessor(invocationFactory, contextProcessor, domainProcessor, exceptionHandler);
        queryProcessor.setCriteriaResolver(new RestCriteriaConstructor());
        return queryProcessor;
    }

    @Bean
    CompileCacheOperation compileCacheOperation(CacheManager cacheManager) {
        return new RdmCompileCacheOperation(new SyncCacheTemplate(cacheManager));
    }

    @Bean
    SourceCacheOperation sourceCacheOperation(CacheManager cacheManager, MetadataRegister metadataRegister) {
        return new RdmSourceCacheOperation(new SyncCacheTemplate(cacheManager), metadataRegister);
    }

}

