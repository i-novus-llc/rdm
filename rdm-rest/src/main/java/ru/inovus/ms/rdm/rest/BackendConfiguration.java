package ru.inovus.ms.rdm.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.util.RdmParamConverterProvider;
import ru.inovus.ms.rdm.util.RowValueMapperPreparer;

@Configuration
public class BackendConfiguration {

    @Autowired
    private FieldFactory fieldFactory;

    @Bean
    RdmParamConverterProvider rdmParamConverterProvider() {
        return new RdmParamConverterProvider();
    }

    @Bean
    RowValueMapperPreparer rowValueMapperPreparer(){
        return new RowValueMapperPreparer();
    }
}
