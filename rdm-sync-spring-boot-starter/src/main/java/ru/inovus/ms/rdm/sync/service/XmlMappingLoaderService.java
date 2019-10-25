package ru.inovus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.sync.model.loader.XmlMapping;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingRefBook;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

public class XmlMappingLoaderService implements MappingLoaderService {

    private static Logger logger = LoggerFactory.getLogger(XmlMappingLoaderService.class);

    private RdmSyncDao rdmSyncDao;

    @Autowired
    private XmlMappingLoaderLockService lockService;

    public XmlMappingLoaderService(RdmSyncDao dao) {
        this.rdmSyncDao = dao;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void load() {
        try {
            if (lockService.tryLock()) {
                try (InputStream io = MappingLoader.class.getResourceAsStream("/rdm-mapping.xml")) {
                    if (io == null) {
                        logger.info("rdm-mapping.xml not found, xml mapping loader skipped");
                        return;
                    }
                    JAXBContext jaxbContext = JAXBContext.newInstance(XmlMapping.class);

                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    XmlMapping mapping = (XmlMapping) jaxbUnmarshaller.unmarshal(io);
                    logger.info("loading ...");
                    mapping.getRefbooks().forEach(this::load);
                    logger.info("xml mapping was loaded");

                } catch (IOException | JAXBException e) {
                    logger.error("xml mapping load error ", e);
                    throw new RdmException(e);
                }
            }
        } finally {
            lockService.releaseLock();
        }
    }

    private void load(XmlMappingRefBook xmlMappingRefBook) {
        if (xmlMappingRefBook.getMappingVersion() > rdmSyncDao.getLastVersion(xmlMappingRefBook.getCode())) {
            logger.info("load {}", xmlMappingRefBook.getCode());
            rdmSyncDao.insertFieldMapping(xmlMappingRefBook.getCode(), xmlMappingRefBook.getFields());
            rdmSyncDao.upsertVersionMapping(xmlMappingRefBook);
            logger.info("mapping for code {} was loaded", xmlMappingRefBook.getCode());
        } else {
            logger.info("mapping for {} not changed", xmlMappingRefBook.getCode());
        }
    }
}
