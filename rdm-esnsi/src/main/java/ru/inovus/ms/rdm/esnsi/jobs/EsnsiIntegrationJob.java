package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.inovus.ms.rdm.esnsi.EsnsiLoader;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class EsnsiIntegrationJob extends AbstractEsnsiDictionaryProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationJob.class);

    @Autowired
    private EsnsiLoader loader;

    @Override
    boolean execute0(JobExecutionContext context) {
        loader.update();
        return false;
    }

}
