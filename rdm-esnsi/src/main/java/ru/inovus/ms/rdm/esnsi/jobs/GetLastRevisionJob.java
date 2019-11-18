package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.*;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionListResponseType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureRequestType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class GetLastRevisionJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        String messageId = jobDataMap.getString(MESSAGE_ID_KEY);
        Map.Entry<GetClassifierRevisionListResponseType, InputStream> getClassifierRevisionList = esnsiSmevClient.getResponse(messageId, GetClassifierRevisionListResponseType.class);
        if (getClassifierRevisionList != null) {
            List<GetClassifierRevisionListResponseType.RevisionDescriptor> revisionDescriptor = getClassifierRevisionList.getKey().getRevisionDescriptor();
            if (!revisionDescriptor.isEmpty()) {
                GetClassifierRevisionListResponseType.RevisionDescriptor last = revisionDescriptor.listIterator(revisionDescriptor.size()).previous();
                int revision = last.getRevision();
                Integer lastDownloadedRevision = esnsiIntegrationDao.getLastVersionRevisionAndCreateNewIfNecessary(classifierCode);
                if (lastDownloadedRevision == null || lastDownloadedRevision < revision) {
                    GetClassifierStructureRequestType getClassifierStructureRequestType = objectFactory.createGetClassifierStructureRequestType();
                    getClassifierStructureRequestType.setRevision(revision);
                    getClassifierStructureRequestType.setCode(classifierCode);
                    AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierStructureRequestType, UUID.randomUUID().toString());
                    JobDetail job = JobBuilder.newJob(GetClassifierStructureJob.class).
                                    withIdentity(GetClassifierStructureJob.class.getSimpleName(), classifierCode).
                                    usingJobData(MESSAGE_ID_KEY, acceptRequestDocument.getMessageId()).
                                    usingJobData(REVISION_KEY, revision).requestRecovery().
                                    build();
                    execSmevResponseResponseReadingJob(job);
                }
            }
            return true;
        }
        return false;
    }

}
