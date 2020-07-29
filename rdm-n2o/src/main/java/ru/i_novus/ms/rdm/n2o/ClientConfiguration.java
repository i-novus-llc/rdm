package ru.i_novus.ms.rdm.n2o;

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
import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.provider.AttributeFilterConverter;
import ru.i_novus.ms.rdm.api.provider.ExportFileProvider;
import ru.i_novus.ms.rdm.api.provider.OffsetDateTimeParamConverter;
import ru.i_novus.ms.rdm.api.provider.RdmMapperConfigurer;
import ru.i_novus.ms.rdm.api.util.RdmPermission;
import ru.i_novus.ms.rdm.n2o.criteria.RestCriteriaConstructor;
import ru.i_novus.ms.rdm.n2o.operation.RdmCompileCacheOperation;
import ru.i_novus.ms.rdm.n2o.operation.RdmSourceCacheOperation;
import ru.i_novus.ms.rdm.n2o.util.RdmPermissionImpl;
import ru.i_novus.ms.rdm.n2o.util.json.RdmN2oLocalDateTimeMapperPreparer;

@Configuration
@EnableJaxRsProxyClient(
        scanPackages = "ru.i_novus.ms.rdm.api.service",
        address = "${rdm.backend.path}"
)
public class ClientConfiguration {

    @Bean
    public RdmPermission rdmPermission() {
        return new RdmPermissionImpl();
    }

    @Bean
    public AttributeFilterConverter attributeFilterConverter() {
        return new AttributeFilterConverter();
    }

    @Bean
    public OffsetDateTimeParamConverter offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    public RdmN2oLocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new RdmN2oLocalDateTimeMapperPreparer();
    }

    @Bean
    public ExportFileProvider exportFileProvider(){
        return new ExportFileProvider();
    }

    @Bean
    public RdmMapperConfigurer rdmMapperConfigurer(){
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
    public CompileCacheOperation compileCacheOperation(CacheManager cacheManager) {
        return new RdmCompileCacheOperation(new SyncCacheTemplate(cacheManager));
    }

    @Bean
    public SourceCacheOperation sourceCacheOperation(CacheManager cacheManager, MetadataRegister metadataRegister) {
        return new RdmSourceCacheOperation(new SyncCacheTemplate(cacheManager), metadataRegister);
    }

}

