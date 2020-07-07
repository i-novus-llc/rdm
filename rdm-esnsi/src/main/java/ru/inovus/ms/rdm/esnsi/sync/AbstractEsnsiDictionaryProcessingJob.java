package ru.inovus.ms.rdm.esnsi.sync;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.EsnsiLoadService;
import ru.inovus.ms.rdm.esnsi.EsnsiLoaderDao;
import ru.inovus.ms.rdm.esnsi.api.ObjectFactory;
import ru.inovus.ms.rdm.esnsi.smev.AdapterClient;

import java.time.Duration;

import static org.quartz.TriggerBuilder.newTrigger;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public abstract class AbstractEsnsiDictionaryProcessingJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEsnsiDictionaryProcessingJob.class);

    private static final String PREV_MESSAGE_ID_KEY = "prevMessageId";
    public static final String NUM_RETRIES_KEY = "numRetries";
    static final String REVISION_KEY = EsnsiLoaderDao.DB_REVISION_FIELD_NAME;
    public static final String MESSAGE_ID_KEY = "messageId";
    public static final String STARTED_AT_KEY = "startedAt";
    private static final String INTERRUPT_KEY = "interruptMePls";

    static final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    AdapterClient adapterClient;

    @Autowired
    EsnsiLoadService esnsiLoadService;

    @Autowired
    private Environment environment;

    @Autowired
    private Scheduler scheduler;

    JobDataMap jobDataMap;

    String classifierCode;

    private JobKey selfIdentity;

    private void init(JobExecutionContext context) {
        this.jobDataMap = context.getJobDetail().getJobDataMap();
        this.selfIdentity = context.getJobDetail().getKey();
        this.classifierCode = context.getJobDetail().getKey().getGroup();
    }

    private boolean checkTimeOutExceeded() {
        if (jobDataMap.containsKey(STARTED_AT_KEY)) {
            long startedAt = jobDataMap.getLong(STARTED_AT_KEY);
            int timeoutMinutes = Integer.parseInt(getProperty("esnsi.sync.job-timeout-minutes"));
            if (Duration.ofMillis(System.currentTimeMillis() - startedAt).toMinutes() > timeoutMinutes) {
                logger.warn("Job {} exceeded timeout", selfIdentity);
                return true;
            }
        }
        return false;
    }

    private boolean checkOutOfDate() {
        boolean outOfDate = false;
        if (getClass() != EsnsiIntegrationJob.class) {
            ClassifierProcessingStage current = esnsiLoadService.getClassifierProcessingStage(classifierCode);
            if (current != getStage(getClass())) {
                outOfDate = true;
                logger.warn("Job with key {} is out of date.", selfIdentity);
            }
        }
        return outOfDate;
    }

    private boolean checkRunOutOfAttempts() {
        int numRetries = 0;
        if (jobDataMap.containsKey(NUM_RETRIES_KEY))
            numRetries = jobDataMap.getInt(NUM_RETRIES_KEY);
        int numRetriesAllowed = Integer.parseInt(getProperty("esnsi.sync.num-retries"));
        boolean runOutOfAttempts = true;
        if (numRetries == 0 || (numRetriesAllowed > 0 && numRetries < numRetriesAllowed))
            runOutOfAttempts = false;
        return runOutOfAttempts;
    }

    private void ackPrevJobMessageId() {
        String prevMessageId = jobDataMap.getString(PREV_MESSAGE_ID_KEY);
        if (prevMessageId != null) {
            adapterClient.acknowledge(prevMessageId);
            jobDataMap.remove(PREV_MESSAGE_ID_KEY);
        }
    }

    @Override
    @SuppressWarnings("squid:S3776")
    public void execute(JobExecutionContext context) throws JobExecutionException {
        init(context);
        boolean outOfDate = checkOutOfDate();
        boolean timeoutExceeded = checkTimeOutExceeded();
        boolean runOutOfAttempts = checkRunOutOfAttempts();
        boolean needToInterrupt = jobDataMap.getBoolean(INTERRUPT_KEY) || outOfDate || timeoutExceeded || runOutOfAttempts;
        if (needToInterrupt && !jobDataMap.containsKey(INTERRUPT_KEY))
            jobDataMap.put(INTERRUPT_KEY, true);
        ackPrevJobMessageId();
        if (!needToInterrupt) {
            try {
                needToInterrupt = execute0(context);
                if (needToInterrupt) {
                    jobDataMap.put(INTERRUPT_KEY, true);
                }
            } catch (Exception e) {
                logger.error("Job {} exceptionally finished.", selfIdentity, e);
                if (getClass() != EsnsiIntegrationJob.class) {
                    int numRetriesAllowed = Integer.parseInt(getProperty("esnsi.sync.num-retries"));
                    int numRetries = (int) jobDataMap.getOrDefault(NUM_RETRIES_KEY, 0);
                    jobDataMap.put(NUM_RETRIES_KEY, numRetries + 1);
                    if (numRetriesAllowed > 0) {
                        logger.info("Job {} will be reexecuted. Retry #{}", selfIdentity, numRetries + 1);
                        throw new JobExecutionException(true);
                    }
                }
            }
        }
        if (needToInterrupt) {
            try {
                this.interrupt();
            } catch (Exception e) {
                logger.error("Execution failed. Retrying until succeed.", e);
                throw new JobExecutionException(true);
            }
        }
    }

    void afterInterrupt() {}

    private static ClassifierProcessingStage getStage(Class<? extends Job> c) {
        if (c == EsnsiIntegrationJob.class) return ClassifierProcessingStage.NONE;
        if (c == GetClassifierRecordsCountJob.class) return ClassifierProcessingStage.GET_RECORDS_COUNT;
        if (c == GetClassifierStructureJob.class) return ClassifierProcessingStage.GET_STRUCTURE;
        if (c == GetDataPageJob.class || c == PagingJob.class) return ClassifierProcessingStage.GET_DATA;
        if (c == GetActualClassifierJob.class) return ClassifierProcessingStage.GET_ACTUAL_CLASSIFIER;
        if (c == SendToRdmJob.class) return ClassifierProcessingStage.SENDING_TO_RDM;
        throw new IllegalStateException("Unexpected Job class.");
    }

    @SuppressWarnings("squid:S00112")
    abstract boolean execute0(JobExecutionContext context) throws Exception;

    void execSmevResponseResponseReadingJob(JobDetail job) {
        int seconds = Integer.parseInt(getProperty("esnsi.sync.job-schedule.seconds"));
        Trigger trigger = newTrigger().startNow().forJob(job).withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(seconds)).build();
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
        job.getJobDataMap().put(STARTED_AT_KEY, System.currentTimeMillis());
        job.getJobDataMap().put(NUM_RETRIES_KEY, 0);
        boolean exec = esnsiLoadService.setClassifierProcessingStageAtomically(
            job.getKey().getGroup(),
            getStage(getClass()),
            getStage(job.getJobClass()),
            () -> {
                scheduler.deleteJob(job.getKey());
                scheduler.scheduleJob(job, trigger);
            }
        );
        if (!exec)
            logger.warn("Unable to execute job with key {}", job.getKey());
    }

    private void interrupt() throws SchedulerException {
        scheduler.deleteJob(selfIdentity);
        logger.info("Job {} successfully interrupted.", selfIdentity);
        afterInterrupt();
    }

    String getProperty(String name) {
        return environment.getProperty(name);
    }

}
