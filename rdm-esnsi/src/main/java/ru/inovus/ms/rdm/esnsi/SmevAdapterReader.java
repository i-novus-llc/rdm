package ru.inovus.ms.rdm.esnsi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.CnsiResponse;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

class SmevAdapterReader implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SmevAdapterReader.class);

    private final BlockingQueue<Map.Entry<String, Object>> requests = new LinkedBlockingQueue<>();
    private final ConcurrentMap<String, Consumer<Map.Entry<Object, InputStream>>> callbacks = new ConcurrentHashMap<>();
    private final EsnsiSmevClient esnsiClient;
    private final List<String> sendedRequests = new LinkedList<>();

    private volatile boolean shutdown;

    SmevAdapterReader(EsnsiSmevClient esnsiClient) {
        this.esnsiClient = esnsiClient;
    }

    @Override
    public void run() {
        while (!shutdown) {
            while (!requests.isEmpty()) {
                Map.Entry<String, Object> request = null;
                try {
                    request = requests.take();
                } catch (InterruptedException e) {
//                      Никогда не выбросится
                    logger.error("Adapter reader was unexpectedly interrupted.", e);
                }
                AcceptRequestDocument acceptRequestDocument = esnsiClient.sendRequest(request.getValue(), request.getKey());
                sendedRequests.add(acceptRequestDocument.getMessageId());
            }
            Iterator<String> sendedRequestsIterator = sendedRequests.iterator();
            while (sendedRequestsIterator.hasNext()) {
                String requestId = sendedRequestsIterator.next();
                Map.Entry<CnsiResponse, InputStream> response = esnsiClient.getResponse(requestId);
                if (response != null) {
                    callbacks.compute(requestId, ((messageId, consumer) -> {
                        consumer.accept(Map.entry(getActualResponse(response.getKey()), response.getValue()));
                        return null;
                    }));
                    sendedRequestsIterator.remove();
                }
            }
        }
    }

    <REQUEST> void sendRequest(String messageId, REQUEST requestData, Consumer<Map.Entry<Object, InputStream>> callback) {
        try {
            requests.put(Map.entry(messageId, requestData));
        } catch (InterruptedException e) {
//              В нормальных условиях не выбросится.
            logger.error("Can't put request.", e);
            return;
        }
        callbacks.put(messageId, callback);
    }

    Object getActualResponse(CnsiResponse response) {
        if (response.getGetAvailableIncrement() != null)
            return response.getGetAvailableIncrement();
        else if (response.getGetChecksumInfo() != null)
            return response.getGetChecksumInfo();
        else if (response.getGetClassifierData() != null)
            return response.getGetClassifierData();
        else if (response.getGetClassifierRecordsCount() != null)
            return response.getGetClassifierRecordsCount();
        else if (response.getGetClassifierRevisionList() != null)
            return response.getGetClassifierRevisionList();
        else if (response.getGetClassifierRevisionsCount() != null)
            return response.getGetClassifierRevisionsCount();
        else if (response.getGetClassifierStructure() != null)
            return response.getGetClassifierStructure();
        else if (response.getListClassifierGroups() != null)
            return response.getListClassifierGroups();
        else if (response.getListClassifiers() != null)
            return response.getListClassifiers();
        else
            return null;
    }

    void shutdown() {
        this.shutdown = true;
    }

}
