package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.EsnsiIntegrationDao;
import ru.inovus.ms.rdm.esnsi.EsnsiSmevClient;
import ru.inovus.ms.rdm.esnsi.api.ObjectFactory;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
abstract class AbstractEsnsiDictionaryProcessingJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEsnsiDictionaryProcessingJob.class);

    private static final String PREV_MESSAGE_ID_KEY = "prevMessageId";
    private static final String NUM_RETRIES_KEY = "numRetries";
    static final String REVISION_KEY = EsnsiIntegrationDao.DB_REVISION_FIELD_NAME;
    static final String MESSAGE_ID_KEY = "messageId";

    static final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    EsnsiSmevClient esnsiSmevClient;

    @Autowired
    EsnsiIntegrationDao esnsiIntegrationDao;

    @Autowired
    private Environment environment;

    @Autowired
    private Scheduler scheduler;

    JobDataMap jobDataMap;

    String classifierCode;

    JobKey selfIdentity;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.jobDataMap = context.getJobDetail().getJobDataMap();
        this.selfIdentity = context.getJobDetail().getKey();
        this.classifierCode = context.getJobDetail().getKey().getGroup();
        boolean outOfDate = false;
        if (getClass() != EsnsiIntegrationJob.class) {
            ClassifierProcessingStage current = esnsiIntegrationDao.getClassifierProcessingStage(classifierCode);
            if (current != getStage(getClass())) {
                outOfDate = true;
                logger.warn("Job with key {} is out of date.", selfIdentity);
            }
        }
        String prevMessageId = jobDataMap.getString(PREV_MESSAGE_ID_KEY);
        if (prevMessageId != null) {
            esnsiSmevClient.acknowledge(prevMessageId);
            jobDataMap.remove(PREV_MESSAGE_ID_KEY);
        }
        int numRetries = 0;
        if (jobDataMap.containsKey(NUM_RETRIES_KEY))
            numRetries = jobDataMap.getInt(NUM_RETRIES_KEY);
        int numRetriesTotal = Integer.parseInt(getProperty("esnsi.sync.num-retries"));
        boolean runOutOfAttempts = true;
        if ((numRetriesTotal <= 0 && numRetries != 1) || (numRetriesTotal > 0 && numRetries < numRetriesTotal))
            runOutOfAttempts = false;
        if (!outOfDate && !runOutOfAttempts) {
            try {
                boolean needToInterrupt = execute0(context);
                if (needToInterrupt)
                    interruptSilently("");
            } catch (Exception e) {
                logger.error("Job {} exceptionally finished.", selfIdentity, e);
                if (getClass() != EsnsiIntegrationJob.class) {
                    logger.info("Job {} will be reexecuted. Retry #{}", selfIdentity, numRetries + 1);
                    jobDataMap.put(NUM_RETRIES_KEY, numRetries + 1);
                    throw new JobExecutionException(true);
                }
            }
        } else {
            if (!outOfDate)
                shutdownPipeline();
            else
                interruptSilently("The pipeline is in another stage, while the given job cannot stop.");
        }
    }

    private void shutdownPipeline() {
        logger.info("Job {} run out of attempts. Pipeline for classifier with code {} will be shutdown.", selfIdentity, classifierCode);
        esnsiIntegrationDao.setClassifierProcessingStage(classifierCode, ClassifierProcessingStage.NONE, this::interrupt);
    }

    void afterInterrupt() {}

    private static ClassifierProcessingStage getStage(Class<? extends Job> c) {
        if (c == EsnsiIntegrationJob.class) return ClassifierProcessingStage.NONE;
        if (c == GetClassifierRecordsCountJob.class) return ClassifierProcessingStage.GET_RECORDS_COUNT;
        if (c == GetClassifierStructureJob.class) return ClassifierProcessingStage.GET_STRUCTURE;
        if (c == GetDataPageJob.class || c == PagingJob.class) return ClassifierProcessingStage.GET_DATA;
        if (c == GetLastRevisionJob.class) return ClassifierProcessingStage.GET_LAST_REVISION;
        if (c == GetRevisionsCountJob.class) return ClassifierProcessingStage.GET_REVISIONS_COUNT;
        if (c == SendToRdmJob.class) return ClassifierProcessingStage.SENDING_TO_RDM;
        throw new IllegalStateException("Unexpected Job class.");
    }

    @SuppressWarnings("squid:S00112")
    abstract boolean execute0(JobExecutionContext context) throws Exception;

    void execSmevResponseResponseReadingJob(JobDetail job) {
        Trigger trigger = newTrigger().startNow().forJob(job).withSchedule(cronSchedule(getProperty("esnsi.smev.adapter.fetch.interval"))).build();
        execJob(job, trigger);
    }

    void execJobWithSimpleSecondlySchedule(JobDetail job) {
        execJob(job, newTrigger().startNow().forJob(job).withSchedule(SimpleScheduleBuilder.repeatSecondlyForever()).build());
    }

    void execJobWithoutSchedule(JobDetail job) {
        execJob(job, newTrigger().startNow().forJob(job).build());
    }

    private void execJob(JobDetail job, Trigger trigger) {
        if (jobDataMap.containsKey(MESSAGE_ID_KEY))
            job.getJobDataMap().put(PREV_MESSAGE_ID_KEY, jobDataMap.get(MESSAGE_ID_KEY));
        job.getJobDataMap().put(NUM_RETRIES_KEY, 0);
        esnsiIntegrationDao.setClassifierProcessingStage(job.getKey().getGroup(), getStage(job.getJobClass()), () -> {
            scheduler.deleteJob(job.getKey());
            scheduler.scheduleJob(job, trigger);
            scheduler.triggerJob(job.getKey());
        });
    }

    void interrupt() throws SchedulerException {
        scheduler.deleteJob(selfIdentity);
        logger.info("Job {} successfully interrupted.", selfIdentity);
        afterInterrupt();
    }

    private void interruptSilently(String payload) {
        try {
            interrupt();
        } catch (SchedulerException e) {
            logger.error("Unable interrupt job with key {}. " + payload + " Please try manually.", selfIdentity, e);
        }
    }

    String getProperty(String name) {
        return environment.getProperty(name);
    }

}
