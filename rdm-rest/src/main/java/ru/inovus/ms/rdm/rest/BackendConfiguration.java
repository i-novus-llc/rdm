package ru.inovus.ms.rdm.rest;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.provider.*;
import ru.inovus.ms.rdm.util.FileNameGenerator;
import ru.inovus.ms.rdm.util.json.LocalDateTimeMapperPreparer;

@Configuration
public class BackendConfiguration {

    @Autowired
    private FieldFactory fieldFactory;

    @Bean
    RdmParamConverterProvider rdmParamConverterProvider() {
        return new RdmParamConverterProvider();
    }

    @Bean
    LocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new LocalDateTimeMapperPreparer();
    }

    @Bean
    ExportFileProvider exportFileProvider(){
        return new ExportFileProvider();
    }

    @Bean
    RdmMapperConfigurer rowValueMapperPreparer(){
        return new RdmMapperConfigurer();
    }

    @Bean("fnsiFileNameGenerator")
    @Primary
    @ConditionalOnProperty(name = "rdm.download.name-generator-class", havingValue = "FnsiFileNameGenerator")
    public FileNameGenerator fnsiFileNameGenerator(){
        return new FnsiFileNameGenerator();
    }

    @Bean
    @ConditionalOnClass(Messages.class)
    NotFoundExceptionMapper notFoundExceptionMapper(Messages messages) {
        return new NotFoundExceptionMapper(messages);
    }

    @Bean
    @Primary
    @ConditionalOnClass(Messages.class)
    UserExceptionMapper userExceptionMapper(Messages messages) {
        return new UserExceptionMapper(messages);
    }

}
