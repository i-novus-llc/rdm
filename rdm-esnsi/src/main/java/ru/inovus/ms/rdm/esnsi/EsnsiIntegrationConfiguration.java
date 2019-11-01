package ru.inovus.ms.rdm.esnsi;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

//@Configuration
@AutoConfigureAfter(LiquibaseAutoConfiguration.class)
public class EsnsiIntegrationConfiguration {

    @Bean
    @DependsOn("liquibase")
    public SpringLiquibase liquibaseRdm(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setDatabaseChangeLogLockTable("databasechangeloglock_rdms");
        liquibase.setChangeLog("classpath*:/esnsi-sync-dm/baseChangelog.xml");
        return liquibase;
    }

}
