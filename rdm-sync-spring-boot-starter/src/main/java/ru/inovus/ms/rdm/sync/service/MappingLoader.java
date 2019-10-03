package ru.inovus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.sync.model.loader.XmlMapping;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingRefBook;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

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
