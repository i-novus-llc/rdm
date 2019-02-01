package ru.inovus.ms.rdm;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.context.ContextProcessor;
import net.n2oapp.framework.api.data.DomainProcessor;
import net.n2oapp.framework.api.data.QueryProcessor;
import net.n2oapp.framework.api.metadata.local.view.widget.util.SubModelQuery;
import net.n2oapp.framework.config.compile.pipeline.N2oEnvironment;
import net.n2oapp.framework.config.util.N2oSubModelsProcessor;
import net.n2oapp.framework.engine.data.N2oInvocationFactory;
import net.n2oapp.framework.engine.data.N2oQueryProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import ru.inovus.ms.rdm.criteria.RestCriteriaConstructor;
import ru.inovus.ms.rdm.provider.ExportFileProvider;
import ru.inovus.ms.rdm.provider.RdmParamConverterProvider;
import ru.inovus.ms.rdm.provider.RowValueMapperPreparer;

import java.util.List;

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
                                         N2oInvocationFactory invocationFactory,
                                         DomainProcessor domainProcessor) {
        N2oQueryProcessor queryProcessor = new N2oQueryProcessor(invocationFactory, contextProcessor, domainProcessor);
        queryProcessor.setCriteriaResolver(new RestCriteriaConstructor());
        return queryProcessor;
    }


    //убрать в 7.0.5
    @Configuration
    static class SubModelProcessorFixConfiguration {
        @Autowired
        QueryProcessor queryProcessor;
        @Autowired
        N2oEnvironment n2oEnvironment;

        @EventListener(ApplicationReadyEvent.class)
        void fix() {
            N2oSubModelsProcessor subModelsProcessor = new N2oSubModelsProcessor(queryProcessor, n2oEnvironment) {
                @Override
                public void executeSubModels(List<SubModelQuery> subQueries, DataSet dataSet) {
                    for (SubModelQuery subQuery : subQueries) {
                        if (subQuery.getLabelFieldId() == null)
                            subQuery.setLabelFieldId("name");
                    }
                    super.executeSubModels(subQueries, dataSet);
                }
            };
            n2oEnvironment.setSubModelsProcessor(subModelsProcessor);
        }
    }

}

