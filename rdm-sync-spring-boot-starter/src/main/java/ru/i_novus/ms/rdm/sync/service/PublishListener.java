package ru.i_novus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import ru.i_novus.ms.rdm.sync.rest.RdmSyncRest;

public class PublishListener {

    private static final Logger logger = LoggerFactory.getLogger(PublishListener.class);

    private final RdmSyncRest rdmSyncRest;
    public PublishListener(RdmSyncRest rdmSyncRest) {
        this.rdmSyncRest = rdmSyncRest;
    }

    @JmsListener(destination = "${rdm_sync.publish.topic:publish_topic}", containerFactory = "publishDictionaryTopicMessageListenerContainerFactory")
    public void onPublish(String refBookCode) {
        logger.info("RefBook with code {} published. Force sync on refBook presence.", refBookCode);
        rdmSyncRest.update(refBookCode);
    }

}
