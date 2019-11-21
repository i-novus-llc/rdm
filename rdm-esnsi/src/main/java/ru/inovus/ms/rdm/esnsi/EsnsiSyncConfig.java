package ru.inovus.ms.rdm.esnsi;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
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
import static org.quartz.TriggerBuilder.newTrigger;

@Configuration
@DependsOn("liquibase")
public class EsnsiSyncConfig {

    @Value("${esnsi.sync.execution.expression}")
    private String esnsiSyncCronExpression;

    @Value("${esnsi.classifier.codes}")
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
                                                     EsnsiLoaderDao dao,
                                                     ApplicationContext applicationContext) throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(quartzProperties());
        factory.setDataSource(dataSource);
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(true);
        JobDetail[] esnsiSyncJobs = getEsnsiSyncAllJob(codes);
        Trigger[] triggers = new Trigger[esnsiSyncJobs.length];
        if (triggers.length > 0) {
            JobDetail syncAllJob = esnsiSyncJobs[0];
            triggers[0] = newTrigger().forJob(syncAllJob).
                          withIdentity(syncAllJob.getKey().getName(), syncAllJob.getKey().getGroup()).
                          withSchedule(cronSchedule(esnsiSyncCronExpression)).
                          build();
        }
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        factory.setJobFactory(jobFactory);
        factory.setJobDetails(esnsiSyncJobs);
        factory.setTriggers(triggers);
        return factory;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private JobDetail[] getEsnsiSyncAllJob(List<String> codes) {
        JobBuilder jb;
        if (!codes.isEmpty()) {
            JobDetail[] jobs = new JobDetail[1];
            JobKey jobKey = getEsnsiSyncJobKey();
            jb = JobBuilder.newJob(EsnsiIntegrationJob.class);
            jb.withIdentity(jobKey);
            jb.storeDurably();
            jb.requestRecovery();
            for (String code : codes)
                jb.usingJobData(code, true);
            jobs[0] = jb.build();
            return jobs;
        }
        return new JobDetail[]{};
    }

    static JobDetail getEsnsiSyncSpecificClassiferJob(String classifierCode) {
        JobBuilder jb = JobBuilder.newJob(EsnsiIntegrationJob.class);
        jb.storeDurably();
        jb.withIdentity(getEsnsiSyncJobKey(classifierCode));
        jb.usingJobData(classifierCode, true);
        return jb.build();
    }

    private static JobKey getEsnsiSyncJobKey(String classifierCode) {
        return JobKey.jobKey("esnsi-sync", classifierCode);
    }

    static JobKey getEsnsiSyncJobKey() {
        return getEsnsiSyncJobKey("NONE");
    }

}
