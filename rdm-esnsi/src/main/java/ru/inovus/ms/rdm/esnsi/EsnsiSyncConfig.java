package ru.inovus.ms.rdm.esnsi;

import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.quartz.*;
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
import ru.inovus.ms.rdm.api.service.*;
import ru.inovus.ms.rdm.esnsi.smev.BufferCleaner;
import ru.inovus.ms.rdm.esnsi.smev.MsgFetcher;
import ru.inovus.ms.rdm.esnsi.sync.EsnsiIntegrationJob;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

@Configuration
@DependsOn("liquibase")
@EnableJaxRsProxyClient(
    classes = {RefBookService.class, DraftService.class, FileStorageService.class, PublishService.class},
    address = "${rdm.backend.path}"
)
public class EsnsiSyncConfig {

    private static final String ESNSI_INTERNAL = "ESNSI-INTERNAL";

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
                                                     ApplicationContext applicationContext,
                                                     @Value("${esnsi.classifier.codes}") List<String> codes,
                                                     @Value("${esnsi.sync.execution.expression}") String esnsiSyncCronExpression,
                                                     @Value("${esnsi.invalid-stage-detector.cron}") String invalidStageDetectorCron,
                                                     @Value("${esnsi.smev.adapter.fetch.interval}") String messageFetcherCron,
                                                     @Value("${esnsi.buffer-cleaner.cron}") String bufferCleanerCron) throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(quartzProperties());
        factory.setDataSource(dataSource);
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(true);
        JobDetail syncAll = getEsnsiSyncAllJob(codes);
        JobDetail[] jobs = {
            syncAll,
            buildJob(InvalidStageDetector.class, "InvalidStageDetector"),
            buildJob(MsgFetcher.class, "MessageFetcher"),
            buildJob(BufferCleaner.class, "BufferCleaner")
        };
        Trigger[] triggers = {
            cronTrigger(jobs[0], esnsiSyncCronExpression),
            cronTrigger(jobs[1], invalidStageDetectorCron),
            cronTrigger(jobs[2], messageFetcherCron),
            cronTrigger(jobs[3], bufferCleanerCron)
        };
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        factory.setJobFactory(jobFactory);
        factory.setJobDetails(jobs);
        factory.setTriggers(triggers);
        return factory;
    }

    private JobDetail getEsnsiSyncAllJob(List<String> codes) {
        JobKey jobKey = JobKey.jobKey("esnsi-sync", "NONE");
        JobBuilder jb = JobBuilder.newJob(EsnsiIntegrationJob.class);
        jb.withIdentity(jobKey);
        jb.storeDurably();
        jb.requestRecovery();
        for (String code : codes)
            jb.usingJobData(code, true);
        return jb.build();
    }

    private JobDetail buildJob(Class<? extends Job> c, String name) {
        JobBuilder jb = JobBuilder.newJob(c);
        JobKey jobKey = JobKey.jobKey(name, EsnsiSyncConfig.ESNSI_INTERNAL);
        return jb.withIdentity(jobKey).storeDurably().requestRecovery().build();
    }

    private Trigger cronTrigger(JobDetail forJob, String cron) {
        return newTrigger().forJob(forJob).
                withIdentity(forJob.getKey().getName(), forJob.getKey().getGroup()).
                withSchedule(cronSchedule(cron)).
                build();
    }

}
