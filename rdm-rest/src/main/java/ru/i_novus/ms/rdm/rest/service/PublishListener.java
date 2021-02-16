package ru.i_novus.ms.rdm.rest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import ru.i_novus.ms.rdm.api.provider.PublishResolver;

import java.util.Collection;

import static org.springframework.util.CollectionUtils.isEmpty;

public class PublishListener {

    private static final Logger logger = LoggerFactory.getLogger(PublishListener.class);

    @Autowired(required = false)
    private Collection<PublishResolver> resolvers;

    @JmsListener(destination = "${rdm.publish.topic:publish_topic}",
            containerFactory = "publishTopicListenerContainerFactory")
    public void onPublish(String refBookCode) {

        logger.info("RefBook with code {} was published.", refBookCode);

        if (!isEmpty(resolvers)) {
            resolvers.forEach(resolver -> resolver.resolve(refBookCode));
        }

        logger.info("RefBook with code {} publish listener was resolved.", refBookCode);
    }
}
