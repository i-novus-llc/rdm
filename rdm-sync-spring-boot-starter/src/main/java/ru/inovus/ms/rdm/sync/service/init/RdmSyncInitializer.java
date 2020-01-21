package ru.inovus.ms.rdm.sync.service.init;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;
import ru.inovus.ms.rdm.sync.service.change_data.RdmSyncExportDirtyRecordsToRdmJob;

import javax.annotation.PostConstruct;
import java.util.List;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
@DependsOn("liquibaseRdm")
class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired private XmlMappingLoaderService mappingLoaderService;
    @Autowired private ClusterLockService clusterLockService;
    @Autowired private RdmSyncDao dao;
    @Autowired private RdmSyncInitializer self;
    @Autowired(required = false) private Scheduler scheduler;

    @Value("${rdm_sync.export_from_local.cron:0/5 * * * * ?}")
    private String exportToRdmJobScanIntervalCron;

    @PostConstruct
    public void start() {
        mappingLoaderService.load();
        addInternal();
        if (scheduler != null) {
            self.setupJobs();
        } else
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
    }

    @Transactional
    public void setupJobs() {
        if (!clusterLockService.tryLock())
            return;
        String group = "RDM_SYNC_INTERNAL";
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey(RdmSyncExportDirtyRecordsToRdmJob.NAME, group));
            JobDetail exportToRdmJob = newJob(RdmSyncExportDirtyRecordsToRdmJob.class).
                    withIdentity(RdmSyncExportDirtyRecordsToRdmJob.NAME, group).
                    build();
            Trigger exportToRdmTrigger = newTrigger().withIdentity(RdmSyncExportDirtyRecordsToRdmJob.NAME, group).forJob(exportToRdmJob).withSchedule(CronScheduleBuilder.cronSchedule(exportToRdmJobScanIntervalCron)).build();
            scheduler.scheduleJob(exportToRdmJob, exportToRdmTrigger);
        } catch (SchedulerException e) {
            logger.error("Cannot schedule {} job. All records in the {} state will remain in it.", RdmSyncExportDirtyRecordsToRdmJob.NAME, RdmSyncLocalRowState.DIRTY, e);
        }
    }

    private void addInternal() {
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping versionMapping : versionMappings) {
            self.addInternal(versionMapping.getTable(), versionMapping.getCode());
        }
    }

    @Transactional
    public void addInternal(String schemaTable, String code) {
        if (!dao.lockRefbookForUpdate(code))
            return;
        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];
        logger.info("Preparing table {} in schema {}.", table, schema);
        dao.addInternalLocalRowStateColumnIfNotExists(schema, table);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schema, table);
        logger.info("Table {} in schema {} successfully prepared.", table, schema);
    }

}
