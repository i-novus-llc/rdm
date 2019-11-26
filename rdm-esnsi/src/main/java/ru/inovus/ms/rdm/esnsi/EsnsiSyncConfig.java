package ru.inovus.ms.rdm.esnsi;

import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
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
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.FileStorageService;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.esnsi.jobs.EsnsiIntegrationJob;
import ru.inovus.ms.rdm.esnsi.jobs.InvalidStageDetector;

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

    @Value("${esnsi.sync.execution.expression}")
    private String esnsiSyncCronExpression;

    @Value("${esnsi.invalid-stage-detector.cron}")
    private String invalidStageDetectorCron;

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
                                                     ApplicationContext applicationContext) throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(quartzProperties());
        factory.setDataSource(dataSource);
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(true);
        JobDetail syncAll = getEsnsiSyncAllJob(codes);
        JobDetail invalidStageDetector = getInvalidStageDetectorJob();
        JobDetail[] jobs = {syncAll, invalidStageDetector};
        Trigger[] triggers = new Trigger[jobs.length];
        triggers[0] = newTrigger().forJob(syncAll).
                      withIdentity(syncAll.getKey().getName(), syncAll.getKey().getGroup()).
                      withSchedule(cronSchedule(esnsiSyncCronExpression)).
                      build();
        triggers[1] = newTrigger().forJob(invalidStageDetector).
                      withIdentity(invalidStageDetector.getKey().getName(), invalidStageDetector.getKey().getGroup()).
                      withSchedule(cronSchedule(invalidStageDetectorCron)).
                      build();
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

    private JobDetail getInvalidStageDetectorJob() {
        JobBuilder jb = JobBuilder.newJob(InvalidStageDetector.class);
        JobKey jobKey = JobKey.jobKey("InvalidStageDetectorJob", "ESNSI-INTERNAL");
        jb.withIdentity(jobKey);
        jb.storeDurably();
        jb.requestRecovery();
        return jb.build();
    }

}
