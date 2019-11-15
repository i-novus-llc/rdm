package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionsCountRequestType;

import java.util.UUID;

@DisallowConcurrentExecution
public class EsnsiIntegrationJob extends AbstractEsnsiDictionaryProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationJob.class);

    @Override
    void execute0(JobExecutionContext context) throws Exception {
        logger.info("Starting EsnsiIntegrationJob.");
        for (String classifierCode : jobDataMap.getKeys()) {
            ClassifierProcessingStage stage = esnsiIntegrationDao.getClassifierProcessingStageAndCreateNewIfNecessary(classifierCode);
            if (stage != ClassifierProcessingStage.NONE) {// Справочник еще обрабатывается
                logger.info("Classifier with code {} is already processing.", classifierCode);
                continue;
            }
            GetClassifierRevisionsCountRequestType getClassifierRevisionsCountRequestType = objectFactory.createGetClassifierRevisionsCountRequestType();
            getClassifierRevisionsCountRequestType.setCode(classifierCode);
            AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRevisionsCountRequestType, UUID.randomUUID().toString());
            JobKey jobKey = JobKey.jobKey(GetRevisionsCountJob.class.getSimpleName(), classifierCode);
            JobDetail job = JobBuilder.newJob(GetRevisionsCountJob.class).
                    withIdentity(jobKey).requestRecovery().
                    usingJobData("messageId", acceptRequestDocument.getMessageId()).build();
            execSmevResponseResponseReadingJob(job);
            logger.info("Job for classifier with code {} was executed.", classifierCode);
        }
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.NONE;
    }

}
