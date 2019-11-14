package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionListResponseType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureRequestType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@DisallowConcurrentExecution
class GetLastRevisionJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    void execute0(JobExecutionContext context) throws Exception {
        String messageId = jobDataMap.getString("messageId");
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
                                    usingJobData("messageId", acceptRequestDocument.getMessageId()).
                                    usingJobData("revision", revision).requestRecovery().
                                    build();
                    execSmevResponseResponseReadingJob(job);
                }
            }
            shutdown();
        }
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.GET_LAST_REVISION;
    }

}
