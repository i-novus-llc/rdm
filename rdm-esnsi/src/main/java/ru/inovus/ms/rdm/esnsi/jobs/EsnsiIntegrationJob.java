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
        for (String dictionaryCode : jobDataMap.getKeys()) {
            ClassifierProcessingStage stage = esnsiIntegrationDao.getClassifierProcessingStageAndCreateNewIfNecessary(dictionaryCode);
            if (stage != ClassifierProcessingStage.NONE) // Справочник еще обрабатывается
                continue;
            GetClassifierRevisionsCountRequestType getClassifierRevisionsCountRequestType = objectFactory.createGetClassifierRevisionsCountRequestType();
            getClassifierRevisionsCountRequestType.setCode(dictionaryCode);
            AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRevisionsCountRequestType, UUID.randomUUID().toString());
            JobKey jobKey = JobKey.jobKey(GetRevisionCountJob.class.getSimpleName(), dictionaryCode);
            JobDetail job = JobBuilder.newJob(GetRevisionCountJob.class).
                    withIdentity(jobKey).requestRecovery().
                    usingJobData("messageId", acceptRequestDocument.getMessageId()).build();
            execSmevResponseResponseReadingJob(job);
            logger.info("Job for classifier with code {} was executed.", dictionaryCode);
        }
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.NONE;
    }

}
