package ru.inovus.ms.rdm.esnsi;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

public class EsnsiIntegrationJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        EsnsiSmevClient esnsiSmevClient;
        try {
            esnsiSmevClient = (EsnsiSmevClient) context.getScheduler().getContext().get(EsnsiSmevClient.class.getSimpleName());
        } catch (SchedulerException e) {
            logger.error("Can't get EsnsiSmevClient bean. Shutting down.", e);
            return;
        }
        SmevAdapterReader reader = new SmevAdapterReader(esnsiSmevClient);
        Executors.newSingleThreadScheduledExecutor().execute(reader);
        reader.shutdown();
    }

}
