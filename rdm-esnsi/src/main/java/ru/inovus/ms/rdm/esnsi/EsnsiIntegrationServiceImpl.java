package ru.inovus.ms.rdm.esnsi;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EsnsiIntegrationServiceImpl implements EsnsiIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationServiceImpl.class);

    @Autowired
    private Scheduler scheduler;

    @Value("${esnsi.classifier.codes}")
    private List<String> codes;

    @Override
    public void update() {
        logger.info("Forcing esnsi sync.");
        try {
            scheduler.triggerJob(EsnsiSyncConfig.getEsnsiSyncJobKey());
        } catch (SchedulerException e) {
            logger.error("Can't start esnsi integration job.", e);
        }
    }

    @Override
    public void update(String classifierCode) {
        logger.info("Forcing sync of {} classifier.", classifierCode);
        try {
            JobDetail job = EsnsiSyncConfig.getEsnsiSyncSpecificClassiferJob(classifierCode);
            scheduler.addJob(job, true);
            scheduler.triggerJob(job.getKey());
        } catch (SchedulerException e) {
            logger.error("Unable to start sync of {} classifier", classifierCode, e);
        }
    }

}
