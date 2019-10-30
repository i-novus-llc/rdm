package ru.inovus.ms.rdm.esnsi;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.inovus.ms.rdm.esnsi.api.CnsiRequest;
import ru.inovus.ms.rdm.esnsi.api.ResponseDocument;
import ru.inovus.ms.rdm.esnsi.api.SmevAdapterFailureException;
import ru.inovus.ms.rdm.esnsi.api.UnknownMessageTypeException;

import javax.xml.bind.JAXBException;
import java.util.UUID;

import static org.springframework.boot.SpringApplication.run;

@SpringBootApplication
public class Application {
    public static void main(String[] args) throws SmevAdapterFailureException, JAXBException, UnknownMessageTypeException, InterruptedException {
        ConfigurableApplicationContext run = run(Application.class);
        EsnsiSmev3Consumer smev3Consumer = run.getBean(EsnsiSmev3Consumer.class);
        CnsiRequest cnsiRequest = CnsiTestRequests.getTestRequest(0);
        smev3Consumer.sendRequest(cnsiRequest, UUID.randomUUID().toString());
        ResponseDocument responseDocument;
        while (true) {
            responseDocument = smev3Consumer.getResponse();
            if (responseDocument != null) {
                smev3Consumer.acknowledge(responseDocument.getOriginalMessageId());
                break;
            }
        }
    }
}
