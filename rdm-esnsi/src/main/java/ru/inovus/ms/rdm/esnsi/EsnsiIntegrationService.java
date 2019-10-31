package ru.inovus.ms.rdm.esnsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.esnsi.api.*;

import javax.xml.datatype.DatatypeConstants;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EsnsiIntegrationService {

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
            GetClassifierDataResponseType data = getData(code, latest.getRevision());
            InputStream inputStream = getInputStream(data);
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
                inputStream
            );
            if (!batch.isEmpty()) {
                dao.insert(batch, struct);
                batch.clear();
            }
            dao.updateLastDownloaded(struct, Timestamp.valueOf(LocalDateTime.now(Clock.systemUTC())));
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
            GetClassifierRevisionListResponseType resp;
            resp = esnsiClient.getResponse(GetClassifierRevisionListResponseType.class, acceptRequestDocument.getMessageId());
            esnsiClient.acknowledge(acceptRequestDocument.getMessageId());
            List<GetClassifierRevisionListResponseType.RevisionDescriptor> revisionDescriptors = resp.getRevisionDescriptor();
            for (GetClassifierRevisionListResponseType.RevisionDescriptor revisionDescriptor : revisionDescriptors) {
                if (latest == null || latest.getTimestamp().compare(revisionDescriptor.getTimestamp()) == DatatypeConstants.GREATER)
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
        return esnsiClient.getResponse(GetClassifierStructureResponseType.class, acceptRequestDocument.getMessageId());
    }

    private GetClassifierDataResponseType getData(String code, int revision) {
        throw new UnsupportedOperationException();
    }

    private InputStream getInputStream(GetClassifierDataResponseType data) {
        throw new UnsupportedOperationException();
    }

}
