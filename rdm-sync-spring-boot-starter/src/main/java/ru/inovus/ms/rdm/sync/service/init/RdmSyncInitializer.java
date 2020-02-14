package ru.inovus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@DependsOn("liquibaseRdm")
class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired
    private XmlMappingLoaderService mappingLoaderService;

    @Autowired
    private RdmSyncDao dao;

    @Autowired(required = false)
    private QuartzConfigurer quartzConfigurer;

    @Autowired
    private LocalTableAutoCreateService localTableAutoCreateService;

    @Autowired
    private InternalInfrastructureCreator internalInfrastructureCreator;

    @Value("${rdm_sync.auto_create.schema:rdm}")
    private String autoCreateSchema;

    @Value("${rdm_sync.auto_create.ref_book_codes:}")
    private List<String> autoCreateRefBookCodes;

    @PostConstruct
    public void start() {
        mappingLoaderService.load();
        autoCreate();
        createInternalInfrastructure();
        if (quartzConfigurer != null) {
            quartzConfigurer.setupJobs();
        } else
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
    }

    private void autoCreate() {
        if (autoCreateRefBookCodes != null) {
            for (String refBookCode : autoCreateRefBookCodes) {
                localTableAutoCreateService.autoCreate(refBookCode, autoCreateSchema);
            }
        }
    }

    private void createInternalInfrastructure() {
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping versionMapping : versionMappings) {
            internalInfrastructureCreator.createInternalInfrastructure(versionMapping.getTable(), versionMapping.getCode(), versionMapping.getDeletedField(), autoCreateRefBookCodes);
        }
    }

}
