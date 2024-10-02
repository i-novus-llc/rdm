package ru.i_novus.ms.rdm.esnsi.smev;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.esnsi.api.ResponseDocument;

import java.io.StringWriter;
import java.time.Clock;
import java.time.LocalDateTime;

@DisallowConcurrentExecution
public class MsgFetcher implements Job {

    private static final Logger logger = LoggerFactory.getLogger(MsgFetcher.class);

    private static final JAXBContext RESPONSE_CTX;

    static {
        try {
            RESPONSE_CTX = JAXBContext.newInstance(ResponseDocument.class);
        } catch (JAXBException e) {
            throw new RdmException(e);
        }
    }

    @Autowired
    private MsgBuffer msgBuffer;

    @Autowired
    private AdapterConsumer adapterConsumer;

    @Value("${esnsi.smev-adapter.message.time-filter-minutes}")
    private int timeFilterMinutes;

    @Value("${esnsi.sync.disable.msg-fetcher:false}")
    private boolean disabled;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (disabled)
            return;
        int n = 0;
        ResponseDocument resp;
        while ((resp = adapterConsumer.getResponseDocument()) != null) {
            StringWriter writer = new StringWriter();
            try {
                RESPONSE_CTX.createMarshaller().marshal(resp, writer);
            } catch (JAXBException e) {
//              Не выбросится
                logger.error("Unexpected error occurred.", e);
                continue;
            }
            String msgId = resp.getSenderProvidedResponseData().getMessageID();
            boolean newMessage = msgBuffer.put(msgId, writer.toString(), LocalDateTime.now(Clock.systemUTC()));
            if (newMessage)
                n++;
            else
                logger.info("Message with id {} is already in buffer.", msgId);
            boolean acknowledged = adapterConsumer.acknowledge(msgId);
            if (!acknowledged)
                logger.info("Message with id {} can't be acknowledged.", msgId);
        }
        logger.info("{} messages fetched from SMEV adapter", n);
    }

}
