package ru.inovus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;

import java.util.List;

@Component
class InternalInfrastructureCreator {

    private static final Logger logger = LoggerFactory.getLogger(InternalInfrastructureCreator.class);

    @Autowired
    private RdmSyncDao dao;

    @Transactional
    public void createInternalInfrastructure(String schemaTable, String code, String isDeletedFieldName, List<String> autoCreateRefBookCodes) {
        if (!dao.lockRefBookForUpdate(code, true))
            return;
        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];
        if (autoCreateRefBookCodes.contains(code)) {
            dao.createSchemaIfNotExists(schema);
            dao.createRefBookTableIfNotExists(schema, table, dao.getFieldMapping(code), isDeletedFieldName);
        }
        logger.info("Preparing table {} in schema {}.", table, schema);
        dao.addInternalLocalRowStateColumnIfNotExists(schema, table);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schema, table);
        logger.info("Table {} in schema {} successfully prepared.", table, schema);
    }

}
