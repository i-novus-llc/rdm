package ru.inovus.ms.rdm.esnsi;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.client.RestTemplate;
import ru.inovus.ms.rdm.esnsi.jobs.EsnsiIntegrationJob;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Configuration
public class EsnsiSyncConfig {

    @Value("${esnsi.sync.execution.expression}")
    private String esnsiSyncCronExpression;

    @Value("${esnsi.dictionary.codes}")
    private List<String> codes;

    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource,
                                                     EsnsiSmevClient esnsiSmevClient,
                                                     EsnsiIntegrationDao dao,
                                                     ApplicationContext applicationContext) throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(quartzProperties());
        factory.setDataSource(dataSource);
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(true);
        JobDetail[] jobDetails = {
            getEsnsiSyncJob(codes)
        };
        Trigger[] triggers = {
            TriggerBuilder.newTrigger().forJob(jobDetails[0])
            .withIdentity(jobDetails[0].getKey().getName())
            .withSchedule(cronSchedule(esnsiSyncCronExpression)).build()
        };
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        factory.setJobFactory(jobFactory);
        factory.setJobDetails(jobDetails);
        factory.setTriggers(triggers);
        return factory;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private JobDetail getEsnsiSyncJob(List<String> codes) {
        JobKey jobKey = getEsnsiSyncJobKey();
        JobBuilder jb = JobBuilder.newJob(EsnsiIntegrationJob.class);
        jb.withIdentity(jobKey);
        for (String code : codes)
            jb.usingJobData(code, true);
        jb.storeDurably();
        jb.requestRecovery();
        return jb.build();
    }

    static JobKey getEsnsiSyncJobKey() {
        return JobKey.jobKey("esnsi-sync", "NONE");
    }

}
