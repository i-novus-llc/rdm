package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionListRequestType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionsCountResponseType;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import static ru.inovus.ms.rdm.esnsi.jobs.EsnsiSyncJobUtils.PAGE_SIZE;

class GetRevisionsCountJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        String messageId = jobDataMap.getString("messageId");
        Map.Entry<GetClassifierRevisionsCountResponseType, InputStream> getClassifierRevisionsCountResponseType = esnsiSmevClient.getResponse(messageId, GetClassifierRevisionsCountResponseType.class);
        if (getClassifierRevisionsCountResponseType != null) {
            int numRevisions = getClassifierRevisionsCountResponseType.getKey().getRevisionsCount();
            int lastPage = numRevisions / PAGE_SIZE;
            GetClassifierRevisionListRequestType getClassifierRevisionListRequestType = objectFactory.createGetClassifierRevisionListRequestType();
            getClassifierRevisionListRequestType.setCode(classifierCode);
            getClassifierRevisionListRequestType.setPageSize(PAGE_SIZE);
            getClassifierRevisionListRequestType.setStartFrom(lastPage * PAGE_SIZE);
            AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRevisionListRequestType, UUID.randomUUID().toString());
            JobDetail job = JobBuilder.newJob(GetLastRevisionJob.class).requestRecovery().
                            withIdentity(JobKey.jobKey(GetLastRevisionJob.class.getSimpleName(), classifierCode)).
                            usingJobData("messageId", acceptRequestDocument.getMessageId()).
                            build();
            execSmevResponseResponseReadingJob(job);
            return true;
        }
        return false;
    }

}
