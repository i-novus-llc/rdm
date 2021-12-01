package ru.i_novus.ms.rdm.web.autoconfigure;

import net.n2oapp.cache.template.SyncCacheTemplate;
import net.n2oapp.framework.api.MetadataEnvironment;
import net.n2oapp.framework.api.data.QueryExceptionHandler;
import net.n2oapp.framework.api.data.QueryProcessor;
import net.n2oapp.framework.api.register.MetadataRegister;
import net.n2oapp.framework.config.compile.pipeline.operation.CompileCacheOperation;
import net.n2oapp.framework.config.compile.pipeline.operation.SourceCacheOperation;
import net.n2oapp.framework.engine.data.N2oInvocationFactory;
import net.n2oapp.framework.engine.data.N2oQueryProcessor;
import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.*;
import ru.i_novus.ms.rdm.api.provider.ExportFileProvider;
import ru.i_novus.ms.rdm.api.provider.RdmMapperConfigurer;
import ru.i_novus.ms.rdm.n2o.config.ClientConfiguration;
import ru.i_novus.ms.rdm.n2o.config.UiStrategyLocatorConfig;
import ru.i_novus.ms.rdm.n2o.criteria.construct.CriteriaConstructResolver;
import ru.i_novus.ms.rdm.n2o.criteria.construct.RestCriteriaConstructor;
import ru.i_novus.ms.rdm.n2o.operation.RdmCompileCacheOperation;
import ru.i_novus.ms.rdm.n2o.operation.RdmSourceCacheOperation;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.util.RefBookAdapter;

import java.util.Collection;

@Configuration
@ConditionalOnProperty(name = "rdm.backend.path")
@ComponentScan({
        "ru.i_novus.ms.rdm.n2o.service",
        "ru.i_novus.ms.rdm.n2o.strategy", "ru.i_novus.ms.rdm.n2o.resolver",
        "ru.i_novus.ms.rdm.n2o.provider", "ru.i_novus.ms.rdm.n2o.transformer"
})
@Import(UiStrategyLocatorConfig.class)
@AutoConfigureAfter({ WebMvcAutoConfiguration.class })
public class RdmWebAutoConfiguration {

    @Bean
    public ExportFileProvider exportFileProvider() {
        return new ExportFileProvider();
    }

    @Bean
    public RdmMapperConfigurer rdmMapperConfigurer() {
        return new RdmMapperConfigurer();
    }

    @Bean
    public RefBookAdapter refBookAdapter(UiStrategyLocator strategyLocator, Messages messages) {
        return new RefBookAdapter(strategyLocator, messages);
    }

    @Configuration
    static class RdmClientConfiguration extends ClientConfiguration {
    }

    @Configuration // based on: N2oConfiguration from rdm-n2o.
    static class RdmN2oConfiguration {

        @Autowired
        private Collection<CriteriaConstructResolver> criteriaConstructResolvers;

        @Bean
        @ConditionalOnMissingBean
        public RestCriteriaConstructor criteriaConstructor() {
            return new RestCriteriaConstructor(criteriaConstructResolvers);
        }

        @Bean
        @ConditionalOnMissingBean
        public QueryProcessor queryProcessor(N2oInvocationFactory invocationFactory,
                                             QueryExceptionHandler exceptionHandler,
                                             MetadataEnvironment metadataEnvironment) {
            N2oQueryProcessor queryProcessor = new N2oQueryProcessor(invocationFactory, exceptionHandler);
            queryProcessor.setEnvironment(metadataEnvironment);
            queryProcessor.setCriteriaResolver(criteriaConstructor());
            return queryProcessor;
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
}
