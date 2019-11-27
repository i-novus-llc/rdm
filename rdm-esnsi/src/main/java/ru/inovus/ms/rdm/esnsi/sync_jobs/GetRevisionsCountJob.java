package ru.inovus.ms.rdm.esnsi.sync_jobs;

import org.quartz.*;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionListRequestType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionsCountResponseType;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import static ru.inovus.ms.rdm.esnsi.sync_jobs.EsnsiSyncJobUtils.PAGE_SIZE;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class GetRevisionsCountJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        String messageId = jobDataMap.getString(MESSAGE_ID_KEY);
        Map.Entry<GetClassifierRevisionsCountResponseType, InputStream> getClassifierRevisionsCountResponseType = adapterClient.getResponse(messageId, GetClassifierRevisionsCountResponseType.class);
        if (getClassifierRevisionsCountResponseType != null) {
            int numRevisions = getClassifierRevisionsCountResponseType.getKey().getRevisionsCount();
            int lastPage = numRevisions / PAGE_SIZE;
            GetClassifierRevisionListRequestType getClassifierRevisionListRequestType = objectFactory.createGetClassifierRevisionListRequestType();
            getClassifierRevisionListRequestType.setCode(classifierCode);
            getClassifierRevisionListRequestType.setPageSize(PAGE_SIZE);
            getClassifierRevisionListRequestType.setStartFrom(lastPage * PAGE_SIZE);
            AcceptRequestDocument acceptRequestDocument = adapterClient.sendRequest(getClassifierRevisionListRequestType, UUID.randomUUID().toString());
            JobDetail job = JobBuilder.newJob(GetLastRevisionJob.class).requestRecovery().
                            withIdentity(JobKey.jobKey(GetLastRevisionJob.class.getSimpleName(), classifierCode)).
                            usingJobData(MESSAGE_ID_KEY, acceptRequestDocument.getMessageId()).
                            build();
            execSmevResponseResponseReadingJob(job);
            return true;
        }
        return false;
    }

}
