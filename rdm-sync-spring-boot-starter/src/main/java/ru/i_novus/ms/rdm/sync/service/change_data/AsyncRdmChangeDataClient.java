package ru.i_novus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class AsyncRdmChangeDataClient extends RdmChangeDataClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRdmChangeDataClient.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${rdm_sync.change_date.queue:rdmChangeData}")
    private String rdmChangeDataQueue;

    @Override
    public <T extends Serializable> void changeData0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Function<? super T, Map<String, Object>> map) {
        try {
            jmsTemplate.convertAndSend(
                rdmChangeDataQueue,
                List.of(Arrays.asList(addUpdate, delete), toRdmChangeDataRequest(refBookCode, addUpdate, delete, map))
            );
        } catch (Exception e) {
            logger.error("An error occurred while sending message to the message broker.", e);
            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }
}
