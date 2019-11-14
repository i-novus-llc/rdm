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
abstract class AbstractEsnsiDictionaryProcessingJob implements InterruptableJob, StatefulJob {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEsnsiDictionaryProcessingJob.class);

    protected final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    protected EsnsiSmevClient esnsiSmevClient;

    @Autowired
    protected EsnsiIntegrationDao esnsiIntegrationDao;

    @Autowired
    private Environment environment;

    protected Scheduler scheduler;

    protected JobDataMap jobDataMap;

    protected String classifierCode;

    protected JobKey selfIdentity;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.scheduler = context.getScheduler();
        this.jobDataMap = context.getJobDetail().getJobDataMap();
        this.selfIdentity = context.getJobDetail().getKey();
        this.classifierCode = context.getJobDetail().getKey().getGroup();
        if (jobDataMap.containsKey("stage_set") && !jobDataMap.getBoolean("stage_set")) {
            ClassifierProcessingStage stage = stage();
            esnsiIntegrationDao.setClassifierProcessingStage(classifierCode, stage);
            jobDataMap.put("stage_set", true);
        }
        String prevMessageId = jobDataMap.getString("prevMessageId");
        if (prevMessageId != null) {
            esnsiSmevClient.acknowledge(prevMessageId);
            jobDataMap.remove("prevMessageId");
        }
        try {
            execute0(context);
        } catch (Exception e) {
            logger.error("Job exceptionally finished.", e);
            if (getClass() != EsnsiIntegrationJob.class) {
                try {
                    esnsiIntegrationDao.setClassifierProcessingStage(classifierCode, ClassifierProcessingStage.NONE);
                    shutdown();
                } catch (SchedulerException ex) {
                    logger.error("Unable to shutdown job. Please try manually.", ex);
                }
            }
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        logger.info("Job {} successfully unscheduled..", getClass().getSimpleName());
    }

    abstract void execute0(JobExecutionContext context) throws Exception;

    abstract ClassifierProcessingStage stage();

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
        if (!job.getJobDataMap().containsKey("stage_set"))
            job.getJobDataMap().put("stage_set", false);
        if (jobDataMap.containsKey("messageId"))
            job.getJobDataMap().put("prevMessageId", jobDataMap.get("messageId"));
        scheduler.deleteJob(job.getKey());
        scheduler.scheduleJob(job, trigger);
        scheduler.triggerJob(job.getKey());
    }

    void shutdown() throws SchedulerException {
        scheduler.interrupt(selfIdentity);
        scheduler.deleteJob(selfIdentity);
    }

    String getProperty(String name) {
        return environment.getProperty(name);
    }

}
