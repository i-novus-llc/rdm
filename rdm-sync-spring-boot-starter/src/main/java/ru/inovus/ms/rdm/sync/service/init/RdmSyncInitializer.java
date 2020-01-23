package ru.inovus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@DependsOn("liquibaseRdm")
class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired private XmlMappingLoaderService mappingLoaderService;
    @Autowired private RdmSyncDao dao;
    @Autowired private RdmSyncInitializer self;
    @Autowired(required = false) private QuartzConfigurer quartzConfigurer;
    @Autowired private RdmSyncRest rdmSyncRest;

    @Value("${rdm_sync.auto_create.schema:rdm}")
    private String autoCreateSchema;

    @Value("#{${rdm_sync.auto_create.ref_book_codes:{}}}")
    private Map<String, String> autoCreateRefBookCodes;

    @PostConstruct
    public void start() {
        List<String> load = mappingLoaderService.load();
        if (autoCreateRefBookCodes != null) {
            for (String refBookCode : autoCreateRefBookCodes.keySet()) {
                if (!load.contains(refBookCode)) {
                    self.autoCreate(refBookCode);
                }
            }
        }
        establishInternalInfrastructure();
        if (quartzConfigurer != null) {
            quartzConfigurer.setupJobs();
        } else
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
    }

    @Transactional
    public void autoCreate(String refBookCode) {
        if (dao.getVersionMapping(refBookCode) != null) {
            logger.info("Skipping auto creation of structures of RefBook with code {}.", refBookCode);
            return;
        }
        logger.info("Auto creating structures of RefBook with code {}", refBookCode);
        RefBook lastPublished;
        try {
            lastPublished = rdmSyncRest.getNewVersionFromRdmThrowOnMissingOrPrimaryKeyMissingOrCompositePrimaryKey(refBookCode);
        } catch (Exception e) {
            logger.error("Error while auto creating structures of RefBook with code {}. Can't get last published version from RDM.", refBookCode, e);
            return;
        }
        Structure structure = lastPublished.getStructure();
        String isDeletedField = "is_deleted";
        boolean isDeletedReserved = structure.getAttributes().stream().anyMatch(attribute -> "is_deleted".equals(attribute.getCode()));
        if (isDeletedReserved)
            isDeletedField = "rdm_sync_internal_" + isDeletedField;
        Structure.Attribute uniqueSysField = structure.getPrimary().get(0);
        XmlMappingRefBook mapping = new XmlMappingRefBook();
        mapping.setCode(refBookCode);
        mapping.setSysTable(String.format("%s.%s", autoCreateSchema, autoCreateRefBookCodes.get(refBookCode)));
        mapping.setDeletedField(isDeletedField);
        mapping.setUniqueSysField(uniqueSysField.getCode());
        mapping.setMappingVersion(-1);
        List<XmlMappingField> fields = new ArrayList<>(structure.getAttributes().size() + 1);
        for (Structure.Attribute attr : structure.getAttributes()) {
            XmlMappingField field = new XmlMappingField();
            field.setRdmField(attr.getCode());
            field.setSysField(attr.getCode());
            field.setSysDataType(DataTypeEnum.getByRdmAttr(attr).getDataTypes().get(0));
            fields.add(field);
        }
        dao.upsertVersionMapping(mapping);
        dao.insertFieldMapping(refBookCode, fields);
        logger.info("Structures of RefBook with code {} auto created.", refBookCode);
    }

    private void establishInternalInfrastructure() {
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping versionMapping : versionMappings) {
            self.establishInternalInfrastructure(versionMapping.getTable(), versionMapping.getCode(), versionMapping.getDeletedField());
        }
    }

    @Transactional
    public void establishInternalInfrastructure(String schemaTable, String code, String isDeletedFieldName) {
        if (!dao.lockRefBookForUpdate(code, true, true))
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
