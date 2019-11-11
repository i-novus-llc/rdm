package ru.inovus.ms.rdm.esnsi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.esnsi.api.*;

import javax.xml.datatype.DatatypeConstants;
import java.io.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EsnsiIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationService.class);

    /**
     * Идентификаторы справочников, которые забираем из ЕСНСИ.
     */
    @Value("${esnsi.dictionary.codes}")
    private List<String> codes;

    private static final int PAGE_SIZE = 100;
    private static final int BATCH_SIZE = 100;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    private EsnsiSmevClient esnsiClient;

    @Autowired
    private EsnsiIntegrationDao dao;

    public void runIntegration() {
        for (String code : codes) {
            GetClassifierRevisionListResponseType.RevisionDescriptor latest = getLastVersionFromEsnsi(code);
            if (latest == null)
                continue;
            Integer lastDownloadedVersionRevision = dao.getLastVersionRevision(code);
            if (lastDownloadedVersionRevision == null || lastDownloadedVersionRevision == latest.getRevision())
                continue;
            GetClassifierStructureResponseType struct = getVersionStructure(code, latest.getRevision());
            dao.createEsnsiVersionDataTable(struct);
            Map.Entry<GetClassifierDataRequestType, InputStream> data = getData(code, latest.getRevision());
            List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
            EsnsiXMLDataFileReadUtil.read(
                row -> {
                    batch.add(row);
                    if (batch.size() == BATCH_SIZE) {
                        dao.insert(batch, struct);
                        batch.clear();
                    }
                },
                struct,
                data.getValue()
            );
            if (!batch.isEmpty()) {
                dao.insert(batch, struct);
                batch.clear();
            }
            dao.updateLastDownloaded(struct, Timestamp.valueOf(LocalDateTime.now(Clock.systemUTC())));
            String fileName = code + "-" + latest.getRevision() + ".xml";
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName))) {
                XmlDataCreator dataCreator = new XmlDataCreator(out, struct);
                dataCreator.init();
                dao.readRows(dataCreator, code, latest.getRevision());
                dataCreator.end();
            } catch (IOException e) {
                logger.error("Can't create file \"" + fileName + "\"", e);
                return;
            }
        }
    }

    private GetClassifierRevisionListResponseType.RevisionDescriptor getLastVersionFromEsnsi(String code) {
        int page = 0;
        GetClassifierRevisionListRequestType req;
        GetClassifierRevisionListResponseType.RevisionDescriptor latest = null;
        do {
            req = objectFactory.createGetClassifierRevisionListRequestType();
            req.setStartFrom(page++);
            req.setPageSize(PAGE_SIZE);
            req.setCode(code);
            AcceptRequestDocument acceptRequestDocument = esnsiClient.sendRequest(req, UUID.randomUUID().toString());
            Map.Entry<GetClassifierRevisionListResponseType, InputStream> resp;
            resp = esnsiClient.getResponse(GetClassifierRevisionListResponseType.class, acceptRequestDocument.getMessageId());
            esnsiClient.acknowledge(acceptRequestDocument.getMessageId());
            List<GetClassifierRevisionListResponseType.RevisionDescriptor> revisionDescriptors = resp.getKey().getRevisionDescriptor();
            for (GetClassifierRevisionListResponseType.RevisionDescriptor revisionDescriptor : revisionDescriptors) {
                if (latest == null || latest.getTimestamp().compare(revisionDescriptor.getTimestamp()) == DatatypeConstants.LESSER)
                    latest = revisionDescriptor;
            }
            if (revisionDescriptors.size() < PAGE_SIZE)
                break;
        } while (true);
        return latest;
    }

    private GetClassifierStructureResponseType getVersionStructure(String code, int revision) {
        GetClassifierStructureRequestType req = objectFactory.createGetClassifierStructureRequestType();
        req.setCode(code);
        req.setRevision(revision);
        AcceptRequestDocument acceptRequestDocument = esnsiClient.sendRequest(req, UUID.randomUUID().toString());
        return esnsiClient.getResponse(GetClassifierStructureResponseType.class, acceptRequestDocument.getMessageId()).getKey();
    }

    private Map.Entry<GetClassifierDataRequestType, InputStream> getData(String code, int revision) {
        GetClassifierDataRequestType req = objectFactory.createGetClassifierDataRequestType();
        req.setCode(code);
        req.setRevision(revision);
        AcceptRequestDocument acceptRequestDocument = esnsiClient.sendRequest(req, UUID.randomUUID().toString());
        return esnsiClient.getResponse(GetClassifierDataRequestType.class, acceptRequestDocument.getMessageId());
    }

}
