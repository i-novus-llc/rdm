package ru.inovus.ms.rdm.esnsi.smev;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@DisallowConcurrentExecution
public class BufferCleaner implements Job {

    private static final Logger logger = LoggerFactory.getLogger(BufferCleaner.class);

    @Autowired
    private MsgBuffer msgBuffer;

    @Autowired
    private AdapterConsumer adapterConsumer;

    @Value("${esnsi.smev-adapter.message.time-filter-minutes}")
    private int timeFilterMinutes;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDateTime bound = LocalDateTime.now(Clock.systemUTC()).minus(timeFilterMinutes, ChronoUnit.MINUTES);
        int n = msgBuffer.removeExpiredMessages(bound);
        logger.info("{} removed from message buffer.", n);
    }

}
