package ru.inovus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@DependsOn("liquibaseRdm")
class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired private XmlMappingLoaderService mappingLoaderService;
    @Autowired private RdmSyncDao dao;
    @Autowired private RdmSyncInitializer self;
    @Autowired(required = false) private QuartzConfigurer quartzConfigurer;

    @PostConstruct
    public void start() {
        mappingLoaderService.load();
        addInternal();
        if (quartzConfigurer != null) {
            quartzConfigurer.setupJobs();
        } else
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
    }

    private void addInternal() {
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping versionMapping : versionMappings) {
            self.addInternal(versionMapping.getTable(), versionMapping.getCode(), versionMapping.getDeletedField());
        }
    }

    @Transactional
    public void addInternal(String schemaTable, String code, String isDeletedFieldName) {
        if (!dao.lockRefBookForUpdate(code, true))
            return;
        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];
        if (!dao.createRefBookTableIfNotExists(schema, table, code, isDeletedFieldName)) {
            logger.warn("Skipping preparing of table {} in schema {}.", table, schema);
            return;
        }
        logger.info("Preparing table {} in schema {}.", table, schema);
        dao.addInternalLocalRowStateColumnIfNotExists(schema, table);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schema, table);
        logger.info("Table {} in schema {} successfully prepared.", table, schema);
    }

}
