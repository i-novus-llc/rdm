package ru.inovus.ms.rdm.esnsi;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class EsnsiIntegrationServiceImpl implements EsnsiIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationServiceImpl.class);

    @Autowired
    private Scheduler scheduler;

    @Value("${esnsi.classifier.codes}")
    private List<String> codes;

    @Autowired
    private EsnsiIntegrationDao dao;

    @Override
    public void update() {
        for (String code : codes) {
            update(code);
        }
    }

    @Override
    public void update(String classifierCode) {
        classifierCode = getCodeIgnoreCase(classifierCode);
        logger.info("Forcing sync of {} classifier.", classifierCode);
        try {
            JobDetail job = EsnsiSyncConfig.getEsnsiSyncSpecificClassiferJob(classifierCode);
            scheduler.addJob(job, true);
            scheduler.triggerJob(job.getKey());
        } catch (SchedulerException e) {
            logger.error("Unable to start sync of {} classifier", classifierCode, e);
            throw new EsnsiSyncException("Unable to start sync of " + classifierCode + " classifier.");
        }
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
        String finalClassifierCode = classifierCode;
        try {
            dao.setClassifierProcessingStage(classifierCode, ClassifierProcessingStage.NONE, () -> {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(finalClassifierCode));
                scheduler.deleteJobs(new ArrayList<>(jobKeys));
            });
        } catch (EsnsiSyncException e) {
            logger.error("Unable to shutdown sync of {} classifier", classifierCode, e);
            throw new EsnsiSyncException("Unable to shutdown sync of " + classifierCode + " classifier.");
        }
    }

    private String getCodeIgnoreCase(String classifierCode) {
        return codes.stream().filter(code -> code.toLowerCase().equals(classifierCode.toLowerCase())).findFirst().orElseThrow(() -> {
            throw new EsnsiSyncException("Can't find classifier with code + " + classifierCode);
        });
    }

}
