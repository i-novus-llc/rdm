package ru.i_novus.ms.rdm.rest.service;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import ru.i_novus.ms.rdm.api.service.VersionDataDiffService;

public class PublishListener {

    private static final Logger logger = LoggerFactory.getLogger(PublishListener.class);

    @Autowired
    private VersionDataDiffService versionDataDiffService;

    @JmsListener(destination = "${rdm.publish.topic:publish_topic}",
            containerFactory = "publishTopicListenerContainerFactory")
    public void onPublish(String refBookCode) {

        logger.info("RefBook with code {} was published.", refBookCode);

        saveLastVersionDataDiff(refBookCode);
    }

    private void saveLastVersionDataDiff(String refBookCode) {
        try {
            versionDataDiffService.saveLastVersionDataDiff(refBookCode);

        } catch (RuntimeException e) {
            logger.error("Save last version data diff error.");
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }
}
