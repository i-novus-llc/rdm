package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.*;
import ru.inovus.ms.rdm.esnsi.PageProcessor;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierDataRequestType;

import java.util.List;
import java.util.UUID;

import static ru.inovus.ms.rdm.esnsi.jobs.EsnsiSyncJobUtils.PAGE_SIZE;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class PagingJob extends AbstractEsnsiDictionaryProcessingJob {


    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt(REVISION_KEY);
        List<PageProcessor> idlePageProcessors = esnsiIntegrationDao.getIdlePageProcessors(classifierCode, revision);
        if (idlePageProcessors.isEmpty())
            return false;
        int numWorkers = Integer.parseInt(getProperty("esnsi.classifier.downloading.num-workers"));
        idlePageProcessors.removeIf(pageProcessor -> pageProcessor.id() > numWorkers);
        boolean flag = false;
        String tableName = jobDataMap.getString("tableName");
        int numRecords = jobDataMap.getInt("numRecords");
        for (PageProcessor pageProcessor : idlePageProcessors) {
            int id = pageProcessor.id();
            int seed = pageProcessor.seed();
            int from = (id - 1) * PAGE_SIZE + seed * numWorkers * PAGE_SIZE;
            if (from < numRecords) {
                flag = true;
                GetClassifierDataRequestType getDataRequest = objectFactory.createGetClassifierDataRequestType();
                getDataRequest.setCode(classifierCode);
                getDataRequest.setPageSize(PAGE_SIZE);
                getDataRequest.setRevision(revision);
                getDataRequest.setStartFrom(from);
                AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getDataRequest, UUID.randomUUID().toString());
                JobDetail job = JobBuilder.newJob(GetDataPageJob.class).withIdentity(GetDataPageJob.class.getSimpleName() + "-" + id, classifierCode).
                                requestRecovery().usingJobData(MESSAGE_ID_KEY, acceptRequestDocument.getMessageId()).
                                usingJobData(REVISION_KEY, revision).
                                usingJobData("tableName", tableName).
                                usingJobData("id", pageProcessor.fullId()).
                                build();
                esnsiIntegrationDao.setPageProcessorBusy(pageProcessor.fullId(), () -> execSmevResponseResponseReadingJob(job));
            }
        }
        if (!flag && idlePageProcessors.size() == numWorkers) {
            JobDetail job = JobBuilder.newJob(SendToRdmJob.class).
                            requestRecovery().
                            usingJobData(REVISION_KEY, revision).
                            withIdentity(SendToRdmJob.class.getSimpleName(), classifierCode).
                            build();
            execJobWithoutSchedule(job);
            return true;
        }
        return false;
    }

}
