package ru.inovus.ms.rdm.esnsi;

import ru.inovus.ms.rdm.esnsi.api.CnsiRequest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

final class CnsiTestRequests {

    private static final int NUM_TEST_REQUESTS = 9;

    private CnsiTestRequests() {}

    static CnsiRequest getTestRequest(int n) {
        if (n < 0 || n >= NUM_TEST_REQUESTS)
            throw new IllegalArgumentException("No request found with id: " + n);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CnsiRequest.class);
            return (CnsiRequest) jaxbContext.createUnmarshaller().unmarshal(Thread.currentThread().getContextClassLoader().getResourceAsStream("test-requests/" + n + "req.xml"));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
