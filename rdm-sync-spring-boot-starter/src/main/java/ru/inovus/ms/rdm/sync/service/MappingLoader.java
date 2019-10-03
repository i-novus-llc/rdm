package ru.inovus.ms.rdm.sync.service;

import javax.annotation.PostConstruct;

public class MappingLoader {

    private MappingLoaderService mappingLoaderService;

    public MappingLoader(MappingLoaderService mappingLoaderService) {
        this.mappingLoaderService = mappingLoaderService;
    }

    @PostConstruct
    public void start() {
        mappingLoaderService.load();
    }


}
