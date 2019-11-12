package ru.inovus.ms.rdm.esnsi;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.esnsi.api.*;

import javax.xml.datatype.DatatypeConstants;
import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EsnsiIntegrationJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationJob.class);

    private static final int PAGE_SIZE = 100;
    private static final int REQUEST_BATCH_SIZE = 300; // Столько реквестов мы одновременно шлем в адаптер при запросе данных.

    private EsnsiSmevClient esnsiSmevClient;
    private ObjectFactory objectFactory;
    private EsnsiIntegrationDao dao;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Sync with esnsi started.");
        try {
            this.esnsiSmevClient = (EsnsiSmevClient) context.getScheduler().getContext().get(EsnsiSmevClient.class.getSimpleName());
        } catch (SchedulerException e) {
            logger.error("Can't get EsnsiSmevClient bean. Shutting down.", e);
            return;
        }
        try {
            this.dao = (EsnsiIntegrationDao) context.getScheduler().getContext().get(EsnsiIntegrationDao.class.getSimpleName());
        } catch (SchedulerException e) {
            logger.error("Can't get EsnsiIntegrationDao bean. Shutting down.", e);
            return;
        }
        this.objectFactory = new ObjectFactory();
        for (String code : context.getJobDetail().getJobDataMap().getKeys()) {
            processDictionary(code);
        }
        logger.info("Esnsi sync complete.");
    }

    private void processDictionary(String code) {
        logger.info("Processing dictionary with code {}", code);
        GetClassifierRevisionsCountRequestType getClassifierRevisionsCountRequest = objectFactory.createGetClassifierRevisionsCountRequestType();
        getClassifierRevisionsCountRequest.setCode(code);
        AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRevisionsCountRequest, UUID.randomUUID().toString());
        Map.Entry<GetClassifierRevisionsCountResponseType, InputStream> getClassifierRevisionsCountResponse;
        do {
            getClassifierRevisionsCountResponse = esnsiSmevClient.getResponse(acceptRequestDocument.getMessageId(), GetClassifierRevisionsCountResponseType.class);
        } while (getClassifierRevisionsCountResponse == null);
        int numRevisions = getClassifierRevisionsCountResponse.getKey().getRevisionsCount();
        if (numRevisions == 0)
            return;
        int revisionsPagesNum = numRevisions % PAGE_SIZE == 0 ? numRevisions / PAGE_SIZE : numRevisions / PAGE_SIZE + 1;
        Map<String, GetClassifierRevisionListRequestType> requests = new HashMap<>();
        for (int i = 0; i < revisionsPagesNum; i++) {
            GetClassifierRevisionListRequestType getClassifierRevisionListRequest = objectFactory.createGetClassifierRevisionListRequestType();
            getClassifierRevisionListRequest.setCode(code);
            getClassifierRevisionListRequest.setPageSize(PAGE_SIZE);
            getClassifierRevisionListRequest.setStartFrom(i * PAGE_SIZE);
            requests.put(UUID.randomUUID().toString(), getClassifierRevisionListRequest);
        }
        GetClassifierRevisionListResponseType.RevisionDescriptor latest = null;
        Map<String, Map.Entry<GetClassifierRevisionListResponseType, InputStream>> map = esnsiSmevClient.batchRequest(requests, GetClassifierRevisionListResponseType.class);
        for (Map.Entry<GetClassifierRevisionListResponseType, InputStream> entry : map.values()) {
            for (GetClassifierRevisionListResponseType.RevisionDescriptor revisionDescriptor : entry.getKey().getRevisionDescriptor()) {
                if (latest == null || latest.getTimestamp().compare(revisionDescriptor.getTimestamp()) == DatatypeConstants.LESSER)
                    latest = revisionDescriptor;
            }
        }
        for (String messageId : map.keySet())
            esnsiSmevClient.acknowledge(messageId);
        if (latest == null)
            return;
        Integer lastDownloadedRevision = dao.getLastVersionRevisionAndCreateNewIfNecessary(code);
        if (lastDownloadedRevision != null && lastDownloadedRevision == latest.getRevision())
            return;
        GetClassifierRecordsCountRequestType getClassifierRecordsCountRequest = objectFactory.createGetClassifierRecordsCountRequestType();
        getClassifierRecordsCountRequest.setCode(code);
        getClassifierRecordsCountRequest.setRevision(latest.getRevision());
        acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRecordsCountRequest, UUID.randomUUID().toString());
        Map.Entry<GetClassifierRecordsCountResponseType, InputStream> getClassifierRecordsCountResponse;
        do {
            getClassifierRecordsCountResponse = esnsiSmevClient.getResponse(acceptRequestDocument.getMessageId(), GetClassifierRecordsCountResponseType.class);
        } while (getClassifierRecordsCountResponse == null);
        int recordsCount = getClassifierRecordsCountResponse.getKey().getRecordsCount();
        int recordsPagesNum = recordsCount % PAGE_SIZE == 0 ? recordsCount / PAGE_SIZE : recordsCount / PAGE_SIZE + 1;
        int recordsBatchesNum = recordsPagesNum % REQUEST_BATCH_SIZE == 0 ? recordsPagesNum / REQUEST_BATCH_SIZE : revisionsPagesNum / REQUEST_BATCH_SIZE + 1;
        Path dictionaryDir = Path.of(code);
        if (!dictionaryDir.toFile().mkdir()) {
            logger.error("Can't create dictionary data directory. Skipping dictionary with code {}", code);
            return;
        }
        byte[] buffer = new byte[4096];
        for (int i = 0; i < recordsBatchesNum; i++) {
            Path batchDir = dictionaryDir.resolve("batch_" + (i + 1));
            if (!batchDir.toFile().mkdir()) {
                logger.error("Can't create batch directory for dictionary with code {}. Skipping batch with num {}", code, i);
                continue;
            }
            int from = i * REQUEST_BATCH_SIZE * PAGE_SIZE;
            Map<String, GetClassifierDataRequestType> batch = new HashMap<>();
            for (int j = 0; j < REQUEST_BATCH_SIZE; j++) {
                GetClassifierDataRequestType getClassifierDataRequest = objectFactory.createGetClassifierDataRequestType();
                getClassifierDataRequest.setCode(code);
                getClassifierDataRequest.setRevision(latest.getRevision());
                getClassifierDataRequest.setStartFrom(from + PAGE_SIZE * j);
                batch.put(UUID.randomUUID().toString(), getClassifierDataRequest);
            }
            Map<String, Map.Entry<GetClassifierDataResponseType, InputStream>> getDataResponse = esnsiSmevClient.batchRequest(batch, GetClassifierDataResponseType.class);
            int page = 1;
            for (Map.Entry<GetClassifierDataResponseType, InputStream> entry : getDataResponse.values()) {
                Path pageFile = batchDir.resolve(page + ".xml");
                try {
                    copy(entry.getValue(), new FileOutputStream(pageFile.toFile()), buffer);
                } catch (FileNotFoundException e) {
                    logger.error("Cannot write to file {}", pageFile.toFile());
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        logger.info("Dictionary with code {} processed.", code);
    }

    private void copy(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
        if (!(inputStream instanceof BufferedInputStream))
            inputStream = new BufferedInputStream(inputStream);
        if (!(outputStream instanceof BufferedOutputStream))
            outputStream = new BufferedOutputStream(outputStream);
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, n);
        }
    }

}
