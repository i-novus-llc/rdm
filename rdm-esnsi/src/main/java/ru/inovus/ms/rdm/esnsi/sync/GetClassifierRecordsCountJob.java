package ru.inovus.ms.rdm.esnsi.sync;

import org.quartz.*;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRecordsCountResponseType;

import java.io.InputStream;
import java.util.Map;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class GetClassifierRecordsCountJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        String messageId = jobDataMap.getString(MESSAGE_ID_KEY);
        Map.Entry<GetClassifierRecordsCountResponseType, InputStream> getClassifierRecordsCountResponseType = adapterClient.getResponse(messageId, GetClassifierRecordsCountResponseType.class);
        if (getClassifierRecordsCountResponseType != null) {
            int revision = jobDataMap.getInt(REVISION_KEY);
            int numRecords = getClassifierRecordsCountResponseType.getKey().getRecordsCount();
            JobDetail job = JobBuilder.newJob(PagingJob.class).
                            withIdentity(PagingJob.class.getSimpleName(), classifierCode).
                            requestRecovery().
                            usingJobData(REVISION_KEY, revision).
                            usingJobData("numRecords", numRecords).
                            usingJobData("publicId", getClassifierRecordsCountResponseType.getKey().getClassifierDescriptor().getPublicId()).
                            build();
            esnsiLoadService.createPageProcessorStateRecords(
                classifierCode,
                revision,
                Integer.parseInt(getProperty("esnsi.classifier.downloading.num-workers"))
            );
            execJobWithSimpleSecondlySchedule(job);
            return true;
        }
        return false;
    }

}
