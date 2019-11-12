package ru.inovus.ms.rdm.esnsi;

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

    @Value("${esnsi.dictionary.codes}")
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

}
