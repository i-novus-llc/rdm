package ru.inovus.ms.rdm;

import net.n2oapp.framework.api.context.ContextProcessor;
import net.n2oapp.framework.api.data.DomainProcessor;
import net.n2oapp.framework.api.data.QueryProcessor;
import net.n2oapp.framework.engine.data.N2oInvocationFactory;
import net.n2oapp.framework.engine.data.N2oQueryProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.criteria.RestCriteriaConstructor;
import ru.inovus.ms.rdm.util.ExportFileProvider;
import ru.inovus.ms.rdm.util.RdmParamConverterProvider;
import ru.inovus.ms.rdm.util.RowValueMapperPreparer;

@Configuration
public class ClientConfiguration {

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
                                         N2oInvocationFactory invocationFactory) {
        N2oQueryProcessor queryProcessor = new N2oQueryProcessor(contextProcessor, domainProcessor, invocationFactory);
        queryProcessor.setCriteriaResolver(new RestCriteriaConstructor());
        return queryProcessor;
    }
}

