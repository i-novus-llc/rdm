package ru.inovus.ms.rdm;

import net.n2oapp.framework.api.context.ContextProcessor;
import net.n2oapp.framework.api.data.DomainProcessor;
import net.n2oapp.framework.api.data.QueryProcessor;
import net.n2oapp.framework.engine.data.factory.ActionInvocationFactory;
import net.n2oapp.framework.engine.processor.QueryProcessorImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.criteria.RefBookCriteriaResolver;
import ru.inovus.ms.rdm.util.ExportFileProvider;
import ru.inovus.ms.rdm.util.RdmParamConverterProvider;
import ru.inovus.ms.rdm.util.RowValueMapperPreparer;



@Configuration
public class ClientConfiguration {
    @Value("${rdm.backend.path}")
    private String restUrl;

    @Bean
    RdmParamConverterProvider rdmParamConverterProvider() {
        return new RdmParamConverterProvider();
    }

    @Bean
    ExportFileProvider exportFileProvider(){
        return new ExportFileProvider();
    }

    @Bean
    RowValueMapperPreparer rowValueMapperPreparer(){
        return new RowValueMapperPreparer();
    }


    @Bean
    public QueryProcessor queryProcessor(ContextProcessor contextProcessor,
                                         DomainProcessor domainProcessor,
                                         ActionInvocationFactory invocationFactory) {
        QueryProcessorImpl queryProcessor = new QueryProcessorImpl(contextProcessor, domainProcessor, invocationFactory);
        queryProcessor.setCriteriaResolver(new RefBookCriteriaResolver());
        return queryProcessor;
    }
}

