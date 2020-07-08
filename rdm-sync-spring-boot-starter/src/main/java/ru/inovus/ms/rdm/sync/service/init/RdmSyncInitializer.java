package ru.inovus.ms.rdm.sync.service.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@DependsOn("liquibaseRdm")
class RdmSyncInitializer {

    @Autowired private XmlMappingLoaderService mappingLoaderService;

    @PostConstruct
    public void start() {
        mappingLoaderService.load();
    }

}
