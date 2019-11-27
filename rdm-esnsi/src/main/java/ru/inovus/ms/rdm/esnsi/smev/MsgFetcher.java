package ru.inovus.ms.rdm.esnsi.smev;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.ResponseDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import static ru.inovus.ms.rdm.esnsi.smev.Utils.EMPTY_INPUT_STREAM;

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

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int n = 0;
        Map.Entry<ResponseDocument, InputStream> resp;
        do {
            resp = adapterConsumer.getResponse();
            if (resp == null)
                break;
            StringWriter writer = new StringWriter();
            try {
                RESPONSE_CTX.createMarshaller().marshal(resp.getKey(), writer);
            } catch (JAXBException e) {
//              Не выбросится
                logger.error("Unexpected error occurred.", e);
                continue;
            }
            byte[] attachment = null;
            if (resp.getValue() != EMPTY_INPUT_STREAM) {
                try {
                    attachment = resp.getValue().readAllBytes();
                } catch (IOException e) {
                    logger.error("Can't read bytes from input stream.", e);
                    continue;
                }
            }
            String msgId = resp.getKey().getSenderProvidedResponseData().getMessageID();
            boolean b = msgBuffer.put(msgId, writer.toString(), LocalDateTime.now(Clock.systemUTC()), attachment);
            if (b)
                n++;
            else
                logger.info("Message with id {} is already in buffer.", msgId);
        } while (true);
        logger.info("{} messages fetched from SMEV adapter", n);
    }

}
