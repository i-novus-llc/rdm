package ru.inovus.ms.rdm.esnsi.sync_jobs;

import org.quartz.*;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRecordsCountRequestType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class GetClassifierStructureJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt(REVISION_KEY);
        String messageId = jobDataMap.getString(MESSAGE_ID_KEY);
        Map.Entry<GetClassifierStructureResponseType, InputStream> getClassifierStructureResponseType = adapterClient.getResponse(messageId, GetClassifierStructureResponseType.class);
        if (getClassifierStructureResponseType != null) {
            GetClassifierStructureResponseType struct = getClassifierStructureResponseType.getKey();
            esnsiLoadService.createEsnsiVersionDataTableAndRemovePreviousIfNecessaryAndSaveStruct(struct);
            GetClassifierRecordsCountRequestType getClassifierRecordsCountRequestType = objectFactory.createGetClassifierRecordsCountRequestType();
            getClassifierRecordsCountRequestType.setCode(classifierCode);
            getClassifierRecordsCountRequestType.setRevision(revision);
            AcceptRequestDocument acceptRequestDocument = adapterClient.sendRequest(getClassifierRecordsCountRequestType, UUID.randomUUID().toString());
            JobDetail job = JobBuilder.newJob(GetClassifierRecordsCountJob.class).
                            usingJobData(REVISION_KEY, revision).
                            usingJobData(MESSAGE_ID_KEY, acceptRequestDocument.getMessageId()).requestRecovery().
                            withIdentity(GetClassifierRecordsCountJob.class.getSimpleName(), classifierCode).build();
            execSmevResponseResponseReadingJob(job);
            return true;
        }
        return false;
    }

}
