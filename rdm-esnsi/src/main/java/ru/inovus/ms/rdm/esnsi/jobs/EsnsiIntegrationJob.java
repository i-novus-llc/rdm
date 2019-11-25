package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionsCountRequestType;

import java.util.Set;
import java.util.UUID;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class EsnsiIntegrationJob extends AbstractEsnsiDictionaryProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationJob.class);

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        logger.info("Starting EsnsiIntegrationJob.");
        for (String classifierCode : jobDataMap.getKeys()) {
            boolean exec;
            ClassifierProcessingStage stage = esnsiIntegrationDao.getClassifierProcessingStageAndCreateNewIfNecessary(classifierCode);
            exec = stage == ClassifierProcessingStage.NONE;
            if (!exec) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(classifierCode));
                if ((selfIdentity.getGroup().equals(classifierCode) && jobKeys.size() == 1) ||
                    (!selfIdentity.getGroup().equals(classifierCode) && jobKeys.size() == 0)) { // Джобов, работающих с этим справочником нету, но stage почему - то не NONE, значит где - то что - то пошло не так
                    exec = true;
                } else {
                    logger.info("Classifier with code {} is already processing.", classifierCode);
                }
            }
            if (!exec)
                continue;
            GetClassifierRevisionsCountRequestType getClassifierRevisionsCountRequestType = objectFactory.createGetClassifierRevisionsCountRequestType();
            getClassifierRevisionsCountRequestType.setCode(classifierCode);
            AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRevisionsCountRequestType, UUID.randomUUID().toString());
            JobKey jobKey = JobKey.jobKey(GetRevisionsCountJob.class.getSimpleName(), classifierCode);
            JobDetail job = JobBuilder.newJob(GetRevisionsCountJob.class).
                    withIdentity(jobKey).requestRecovery().
                    usingJobData(MESSAGE_ID_KEY, acceptRequestDocument.getMessageId()).build();
            execSmevResponseResponseReadingJob(job);
            logger.info("Job for classifier with code {} was executed.", classifierCode);
        }
        return false;
    }

}
