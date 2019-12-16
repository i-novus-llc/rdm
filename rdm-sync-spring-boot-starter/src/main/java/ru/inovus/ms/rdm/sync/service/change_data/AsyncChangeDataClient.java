package ru.inovus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.exception.RdmException;

import java.util.List;
import java.util.Map;

@Service
public class AsyncChangeDataClient implements ChangeDataClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncChangeDataClient.class);

    @Autowired(required = false)
    private JmsTemplate jmsTemplate;

    @Value("${rdm_sync.change_date.queue:changeData}")
    private String changeDataQueue;

    @Autowired
    private ChangeDataRequestCallback callback;

    @Override
    public void changeData(String refBookCode, List<Object> addUpdate, List<Object> delete) {
        if (jmsTemplate != null) {
            try {
                jmsTemplate.convertAndSend(
                    changeDataQueue,
                    Map.of(
                        "src", List.of(addUpdate, delete),
                        "converted", ChangeDataClient.convertToChangeDataRequest(refBookCode, addUpdate, delete)
                    )
                );
            } catch (Exception e) {
                logger.error("An error occurred while sending message to the message broker.", e);
                callback.onError(refBookCode, addUpdate, delete, e);
            }
        } else {
            String msg = "Message queue is not configured. Async pull request can't be performed.";
            callback.onError(refBookCode, addUpdate, delete, new RdmException(msg));
        }
    }
}
