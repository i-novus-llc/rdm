package ru.inovus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Service
public class AsyncRdmChangeDataClient extends RdmChangeDataClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRdmChangeDataClient.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${rdm_sync.change_date.queue:rdmChangeData}")
    private String rdmChangeDataQueue;



    @Override
    public <T extends Serializable> void changeData(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {
        try {
            jmsTemplate.convertAndSend(
                    rdmChangeDataQueue,
                List.of(Arrays.asList(addUpdate, delete), Utils.convertToRdmChangeDataRequest(refBookCode, addUpdate, delete))
            );
        } catch (Exception e) {
            logger.error("An error occurred while sending message to the message broker.", e);
            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }
}
