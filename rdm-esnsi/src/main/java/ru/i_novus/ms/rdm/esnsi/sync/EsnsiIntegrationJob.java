package ru.i_novus.ms.rdm.esnsi.sync;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.esnsi.EsnsiLoader;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class EsnsiIntegrationJob extends AbstractEsnsiDictionaryProcessingJob {

    @Autowired
    private EsnsiLoader loader;

    @Override
    boolean execute0(JobExecutionContext context) {
        loader.update();
        return false;
    }

}
