package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.PageProcessor;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierDataRequestType;

import java.util.List;
import java.util.UUID;

import static ru.inovus.ms.rdm.esnsi.EsnsiIntegrationDao.getClassifierSpecificDataTableName;
import static ru.inovus.ms.rdm.esnsi.jobs.EsnsiSyncJobUtils.PAGE_SIZE;

@DisallowConcurrentExecution
class PagingJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    void execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt("revision");
        List<PageProcessor> idlePageProcessors = esnsiIntegrationDao.getIdlePageProcessors(classifierCode, revision);
        if (idlePageProcessors.isEmpty())
            return;
        int numWorkers = Integer.parseInt(getProperty("esnsi.classifier.downloading.num-workers"));
        idlePageProcessors.removeIf(pageProcessor -> pageProcessor.id() > numWorkers);
        boolean flag = false;
        String tableName = getClassifierSpecificDataTableName(classifierCode, revision);
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
                                requestRecovery().usingJobData("messageId", acceptRequestDocument.getMessageId()).
                                usingJobData("revision", revision).
                                usingJobData("stage_set", true).
                                usingJobData("busy_set", false).
                                usingJobData("tableName", tableName).
                                usingJobData("id", id).
                                build();
                execSmevResponseResponseReadingJob(job);
            }
        }
        if (!flag && idlePageProcessors.size() == numWorkers) {
            JobDetail job = JobBuilder.newJob(SendToRdmJob.class).
                            requestRecovery().
                            withIdentity(SendToRdmJob.class.getSimpleName(), classifierCode).
                            build();
            execJobWithoutSchedule(job);
            shutdown();
        }
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.GET_DATA;
    }

}
