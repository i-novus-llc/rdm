package ru.inovus.ms.rdm.esnsi;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.CnsiResponse;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
public class EsnsiIntegrationServiceImpl implements EsnsiIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationServiceImpl.class);

    @Autowired
    private Scheduler scheduler;

    @Value("${esnsi.dictionary.codes}")
    private List<String> codes;

    @Override
    public void update() {
        JobDetail esnsiSyncJob = EsnsiSyncConfig.getEsnsiSyncJob(codes);
        try {
            scheduler.addJob(esnsiSyncJob, true);
            scheduler.triggerJob(esnsiSyncJob.getKey());
        } catch (SchedulerException e) {
            logger.error("Can't start esnsi integration job.", e);
        }
    }

    static class IntegrationJob implements Job {

        private static final Logger logger = LoggerFactory.getLogger(IntegrationJob.class);

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            EsnsiSmevClient esnsiSmevClient;
            try {
                esnsiSmevClient = (EsnsiSmevClient) context.getScheduler().getContext().get(EsnsiSmevClient.class.getSimpleName());
            } catch (SchedulerException e) {
                logger.error("Can't get EsnsiSmevClient bean. Shutting down.", e);
                return;
            }
            SmevAdapterQueueReader reader = new SmevAdapterQueueReader(esnsiSmevClient);
            Executors.newSingleThreadScheduledExecutor().execute(reader);
        }

    }

    private static class SmevAdapterQueueReader implements Runnable {

        private static final Logger logger = LoggerFactory.getLogger(SmevAdapterQueueReader.class);

        private final BlockingQueue<Map.Entry<String, Object>> requests = new LinkedBlockingQueue<>();
        private final ConcurrentMap<String, Consumer<Map.Entry<Object, InputStream>>> callbacks = new ConcurrentHashMap<>();
        private final EsnsiSmevClient esnsiClient;
        private final List<String> sendedRequests = new LinkedList<>();

        private volatile boolean shutdown;

        SmevAdapterQueueReader(EsnsiSmevClient esnsiClient) {
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

}
