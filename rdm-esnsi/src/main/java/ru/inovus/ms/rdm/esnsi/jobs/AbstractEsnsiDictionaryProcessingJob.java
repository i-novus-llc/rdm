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

@DisallowConcurrentExecution
abstract class AbstractEsnsiDictionaryProcessingJob implements StatefulJob {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEsnsiDictionaryProcessingJob.class);

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

    private JobKey selfIdentity;

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
                try {
                    interrupt();
                } catch (SchedulerException e) {
                    logger.error("Unable to interrupt job with key {}. The pipeline is in another stage, while the given job cannot stop. Please try manually.", selfIdentity, e);
                }
            }
        }
        String prevMessageId = jobDataMap.getString("prevMessageId");
        if (prevMessageId != null) {
            esnsiSmevClient.acknowledge(prevMessageId);
            jobDataMap.remove("prevMessageId");
        }
        int numRetries = 0;
        if (jobDataMap.containsKey("numRetries"))
            numRetries = jobDataMap.getInt("numRetries");
        int numRetriesTotal = Integer.parseInt(getProperty("esnsi.sync.num-retries"));
        if (!outOfDate && numRetries <= numRetriesTotal) {
            try {
                boolean needToInterrupt = execute0(context);
                if (needToInterrupt) {
                    try {
                        interrupt();
                    } catch (SchedulerException e) {
                        logger.error("Unable interrupt job with key {}. Please try manually.", selfIdentity, e);
                    }
                }
            } catch (Exception e) {
                logger.error("Job {} exceptionally finished.", selfIdentity, e);
                if (getClass() != EsnsiIntegrationJob.class) {
                    logger.info("Job {} will be reexecuted. Retry #{}", selfIdentity, numRetries + 1);
                    jobDataMap.put("numRetries", numRetries + 1);
                    throw new JobExecutionException(true);
                }
            }
        } else {
            if (!outOfDate)
                shutdownPipeline();
            else {
                try {
                    interrupt();
                } catch (SchedulerException e) {
                    logger.error("Unable interrupt job with key {}. Please try manually.", selfIdentity, e);
                }
            }
        }
    }

    private void shutdownPipeline() {
        logger.info("Job {} run out of attempts. Pipeline for classifier with code {} will be shutdown.", selfIdentity, classifierCode);
        esnsiIntegrationDao.setClassifierProcessingStage(classifierCode, ClassifierProcessingStage.NONE, () -> interrupt());
    }

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

    abstract boolean execute0(JobExecutionContext context) throws Exception;

    void execSmevResponseResponseReadingJob(JobDetail job) throws SchedulerException {
        Trigger trigger = newTrigger().startNow().forJob(job).withSchedule(cronSchedule(getProperty("esnsi.smev.adapter.fetch.interval"))).build();
        execJob(job, trigger);
    }

    void execJobWithSimpleSecondlySchedule(JobDetail job) throws SchedulerException {
        execJob(job, newTrigger().startNow().forJob(job).withSchedule(SimpleScheduleBuilder.repeatSecondlyForever()).build());
    }

    void execJobWithoutSchedule(JobDetail job) throws SchedulerException {
        execJob(job, newTrigger().startNow().forJob(job).build());
    }

    private void execJob(JobDetail job, Trigger trigger) throws SchedulerException {
        if (jobDataMap.containsKey("messageId"))
            job.getJobDataMap().put("prevMessageId", jobDataMap.get("messageId"));
        job.getJobDataMap().put("numRetries", 0);
        esnsiIntegrationDao.setClassifierProcessingStage(job.getKey().getGroup(), getStage(job.getJobClass()), () -> {
            scheduler.deleteJob(job.getKey());
            scheduler.scheduleJob(job, trigger);
            scheduler.triggerJob(job.getKey());
        });
    }

    void interrupt() throws SchedulerException {
        scheduler.deleteJob(selfIdentity);
        logger.info("Job {} successfully interrupted.", selfIdentity);
    }

    String getProperty(String name) {
        return environment.getProperty(name);
    }

}
