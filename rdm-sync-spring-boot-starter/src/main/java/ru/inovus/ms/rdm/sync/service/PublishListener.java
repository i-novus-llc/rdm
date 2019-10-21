package ru.inovus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;

public class PublishListener {

    private static final Logger logger = LoggerFactory.getLogger(PublishListener.class);

    private final RdmSyncRest rdmSyncRest;
    public PublishListener(RdmSyncRest rdmSyncRest) {
        this.rdmSyncRest = rdmSyncRest;
    }

    @JmsListener(destination = "${jms.publish.topic}")
    public void onMessage(String refBookCode) {
        logger.info("RefBook with code {} published. Force sync.", refBookCode);
        rdmSyncRest.update(refBookCode);
    }

}
