package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRecordsCountResponseType;

import java.io.InputStream;
import java.util.Map;

@DisallowConcurrentExecution
class GetClassifierRecordsCountJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        String messageId = jobDataMap.getString("messageId");
        Map.Entry<GetClassifierRecordsCountResponseType, InputStream> getClassifierRecordsCountResponseType = esnsiSmevClient.getResponse(messageId, GetClassifierRecordsCountResponseType.class);
        if (getClassifierRecordsCountResponseType != null) {
            int revision = jobDataMap.getInt("revision");
            int numRecords = getClassifierRecordsCountResponseType.getKey().getRecordsCount();
            JobDetail job = JobBuilder.newJob(PagingJob.class).
                            withIdentity(PagingJob.class.getSimpleName(), classifierCode).
                            requestRecovery().
                            usingJobData("revision", revision).
                            usingJobData("numRecords", numRecords).
                            build();
            esnsiIntegrationDao.createPageProcessorStateRecords(
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
