package ru.i_novus.ms.rdm.sync.service.init;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;
import ru.i_novus.ms.rdm.sync.service.change_data.RdmSyncExportDirtyRecordsToRdmJob;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
@ConditionalOnClass(name = "org.quartz.Scheduler")
class QuartzConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(QuartzConfigurer.class);

    @Autowired(required = false)
    private Scheduler scheduler;

    @Autowired
    private ClusterLockService clusterLockService;

    @Value("${rdm_sync.export_from_local.cron:0/5 * * * * ?}")
    private String exportToRdmJobScanIntervalCron;

    @Value("${rdm_sync.change_data_mode}")
    private String changeDataMode;

    @Transactional
    public void setupJobs() {
        if (!clusterLockService.tryLock())
            return;
        String group = "RDM_SYNC_INTERNAL";
        try {
            if (!scheduler.getMetaData().isJobStoreClustered())
                logger.warn("Scheduler configured in non clustered mode. There may be concurrency issues.");
            JobKey exportToRdmJobKey = JobKey.jobKey(group, RdmSyncExportDirtyRecordsToRdmJob.NAME);
            if (changeDataMode != null) {
                TriggerKey exportToRdmTriggerKey = TriggerKey.triggerKey(exportToRdmJobKey.getName(), exportToRdmJobKey.getGroup());
                Trigger exportToRdmExistingTrigger = scheduler.getTrigger(exportToRdmTriggerKey);
                JobDetail exportToRdmJob = newJob(RdmSyncExportDirtyRecordsToRdmJob.class).
                        withIdentity(exportToRdmJobKey).
                        build();
                Trigger exportToRdmTrigger = newTrigger().
                        withIdentity(exportToRdmTriggerKey).
                        forJob(exportToRdmJob).
                        withSchedule(CronScheduleBuilder.cronSchedule(exportToRdmJobScanIntervalCron)).
                        build();
                if (exportToRdmExistingTrigger == null) {
                    scheduler.scheduleJob(exportToRdmJob, exportToRdmTrigger);
                } else {
                    if (exportToRdmExistingTrigger instanceof CronTrigger) {
                        CronTrigger ct = (CronTrigger) exportToRdmExistingTrigger;
                        if (!ct.getCronExpression().equals(exportToRdmJobScanIntervalCron)) {
                            scheduler.rescheduleJob(exportToRdmTriggerKey, exportToRdmTrigger);
                        } else
                            logger.info("Trigger's {} expression not changed.", exportToRdmTriggerKey);
                    } else
                        logger.warn("Trigger {} is not CronTrigger instance. Leave it as it is.", exportToRdmTriggerKey);
                }
            } else {
                scheduler.deleteJob(exportToRdmJobKey);
            }
        } catch (SchedulerException e) {
            logger.error("Cannot schedule {} job. All records in the {} state will remain in it.", RdmSyncExportDirtyRecordsToRdmJob.NAME, RdmSyncLocalRowState.DIRTY, e);
        }
    }

}
