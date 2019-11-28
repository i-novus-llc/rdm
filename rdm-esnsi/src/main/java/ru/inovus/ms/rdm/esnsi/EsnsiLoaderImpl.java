package ru.inovus.ms.rdm.esnsi;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionsCountRequestType;
import ru.inovus.ms.rdm.esnsi.api.ObjectFactory;
import ru.inovus.ms.rdm.esnsi.jobs.GetRevisionsCountJob;

import java.util.List;
import java.util.UUID;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static ru.inovus.ms.rdm.esnsi.jobs.AbstractEsnsiDictionaryProcessingJob.*;

@Service
public class EsnsiLoaderImpl implements EsnsiLoader {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiLoaderImpl.class);

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @Autowired
    private Scheduler scheduler;

    @Value("${esnsi.classifier.codes}")
    private List<String> codes;

    @Value("${esnsi.smev.adapter.fetch.interval}")
    private String fetchInterval;

    @Autowired
    private EsnsiLoadService esnsiLoadService;

    @Autowired
    private EsnsiSmevClient esnsiSmevClient;

    @Override
    public void update() {
        logger.info("Starting EsnsiIntegrationJob.");
        for (String code : codes) {
            update(code);
        }
    }

    @Override
    public String update(String classifierCode) {
        classifierCode = getCodeIgnoreCase(classifierCode);
        logger.info("Forcing sync of {} classifier.", classifierCode);
        GetClassifierRevisionsCountRequestType getClassifierRevisionsCountRequestType = OBJECT_FACTORY.createGetClassifierRevisionsCountRequestType();
        getClassifierRevisionsCountRequestType.setCode(classifierCode);
        AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRevisionsCountRequestType, UUID.randomUUID().toString());
        JobKey jobKey = JobKey.jobKey(GetRevisionsCountJob.class.getSimpleName(), classifierCode);
        JobDetail job = JobBuilder.newJob(GetRevisionsCountJob.class).
                withIdentity(jobKey).requestRecovery().
                usingJobData(MESSAGE_ID_KEY, acceptRequestDocument.getMessageId()).build();
        Trigger trigger = newTrigger().startNow().forJob(job).withSchedule(cronSchedule(fetchInterval)).build();
        try {
            execJob(job, trigger);
        } catch (Exception e) {
            logger.error("Unable to start sync of {} classifier", classifierCode, e);
            throw new RdmException("Unable to start sync of " + classifierCode + " classifier.");
        }
        logger.info("Job for classifier with code {} was executed.", classifierCode);
        return acceptRequestDocument.getMessageId();
    }

    private void execJob(JobDetail job, Trigger trigger) {
        job.getJobDataMap().put(STARTED_AT_KEY, System.currentTimeMillis());
        job.getJobDataMap().put(NUM_RETRIES_KEY, 0);
        esnsiLoadService.setClassifierProcessingStageAtomically(
            job.getKey().getGroup(),
            ClassifierProcessingStage.NONE,
            ClassifierProcessingStage.GET_REVISIONS_COUNT,
            () -> {
                scheduler.deleteJob(job.getKey());
                scheduler.scheduleJob(job, trigger);
                scheduler.triggerJob(job.getKey());
            }
        );
    }

    @Override
    public void shutdown() {
        for (String classifierCode : codes) {
            shutdown(classifierCode);
        }
    }

    @Override
    public void shutdown(String classifierCode) {
        classifierCode = getCodeIgnoreCase(classifierCode);
        try {
            esnsiLoadService.setClassifierProcessingStageAtomically(classifierCode, null, ClassifierProcessingStage.NONE, () -> {});
        } catch (Exception e) {
            logger.error("Unable to shutdown sync of {} classifier", classifierCode, e);
            throw new RdmException("Unable to shutdown sync of " + classifierCode + " classifier.");
        }
    }

    @Override
    public void cleanHistory() {
        for (String classifierCode : codes) {
            cleanHistory(classifierCode);
        }
    }

    @Override
    public void cleanHistory(String classifierCode) {
        classifierCode = getCodeIgnoreCase(classifierCode);
        esnsiLoadService.cleanClassifierSyncHistory(classifierCode);
    }

    private String getCodeIgnoreCase(String classifierCode) {
        return codes.stream().filter(code -> code.equalsIgnoreCase(classifierCode)).findFirst().orElseThrow(() -> {
            throw new RdmException("Can't find classifier with code + " + classifierCode);
        });
    }

}
