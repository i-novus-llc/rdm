package ru.inovus.ms.rdm.rest;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendConfiguration {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    RdmParamConverterProvider rdmParamConverterProvider() {
        return new RdmParamConverterProvider();
    }
}
