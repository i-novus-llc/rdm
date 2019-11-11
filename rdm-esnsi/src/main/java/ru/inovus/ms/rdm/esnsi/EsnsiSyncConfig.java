package ru.inovus.ms.rdm.esnsi;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Configuration
public class EsnsiSyncConfig {

    @Autowired
    private EsnsiSmevClient esnsiSmevClient;

    @Value("${esnsi.sync.execution.expression}")
    private String esnsiSyncCronExpression;

    @Value("${esnsi.dictionary.codes}")
    private List<String> codes;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        Properties properties = new Properties();
        properties.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        properties.put("org.quartz.jobStore.isClustered", true);
        factory.setQuartzProperties(properties);
        factory.setDataSource(dataSource);
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(false);
        JobDetail[] jobDetails = {
            getEsnsiSyncJob(codes)
        };
        Trigger[] triggers = {
            TriggerBuilder.newTrigger().forJob(jobDetails[0])
            .withIdentity(jobDetails[0].getKey().getName())
            .withSchedule(cronSchedule(esnsiSyncCronExpression))
            .build()
        };
        factory.setJobDetails(jobDetails);
        factory.setTriggers(triggers);
        Map<String, Object> jobBeans = new HashMap<>();
        jobBeans.put(EsnsiSmevClient.class.getSimpleName(), esnsiSmevClient);
        factory.setSchedulerContextAsMap(jobBeans);
        return factory;
    }

    private JobDetail getEsnsiSyncJob(List<String> codes) {
        JobKey jobKey = getEsnsiSyncJobKey();
        JobBuilder jb = JobBuilder.newJob(EsnsiIntegrationJob.class);
        jb.withIdentity(jobKey);
        for (String code : codes)
            jb.usingJobData(code, true);
        jb.storeDurably();
        return jb.build();
    }

    static JobKey getEsnsiSyncJobKey() {
        return JobKey.jobKey("esnsi-sync");
    }




}
