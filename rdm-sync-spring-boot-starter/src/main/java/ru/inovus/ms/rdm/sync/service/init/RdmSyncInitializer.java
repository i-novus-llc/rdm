package ru.inovus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.MappingLoaderService;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import static ru.inovus.ms.rdm.api.util.StringUtils.addDoubleQuotes;
import static ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState.RDM_SYNC_INTERNAL_STATE_COLUMN;

@Component
class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    private static final String INTERNAL_FUNCTION = "rdm_sync_internal()";
    private static final String LOCAL_ROW_STATE_UPDATE_FUNC =
    "CREATE OR REPLACE FUNCTION\n" +
    "   %1$s\n" +
    "RETURNS TRIGGER AS\n" +
    "   $$\n" +
    "       BEGIN\n" +
	"			IF (NEW.%2$s IS NULL) THEN\n" +
    "           	NEW.%2$s = '%3$s';\n" +
	"			END IF;\n" +
    "           RETURN NEW;\n" +
    "       END;\n" +
    "   $$\n" +
    "LANGUAGE 'plpgsql'";

    @Autowired private MappingLoaderService mappingLoaderService;
    @Autowired private RdmSyncDao dao;
    @Autowired private RdmSyncInitializer self;
    @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

    @PostConstruct
    public void start() {
        mappingLoaderService.load();
        addInternal();
    }

    private void addInternal() {
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping versionMapping : versionMappings) {
            self.addInternal(versionMapping.getTable(), versionMapping.getCode(), versionMapping.getPrimaryField());
        }
    }

    @Transactional
    public void addInternal(String schemaTable, String code, String pk) {
        if (!dao.lockRefbookForUpdate(code))
            return;
        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];
        logger.info("Preparing table {} in schema {}.", table, schema);
        addInternalLocalRowStateColumnIfNotExists(schemaTable);
        createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию
        createTriggerOnInsertOrUpdate(schemaTable, schema, table);
        addUniqueConstraint(schemaTable, schema, table, pk);
        logger.info("Table {} in schema {} successfully prepared.", table, schema);
    }

    private void addUniqueConstraint(String schemaTable, String schema, String table, String pk) {
        boolean exists = false;
        List<String> constraints = jdbcTemplate.query("SELECT constraint_name FROM information_schema.constraint_column_usage WHERE table_schema = :schema AND table_name = :table AND column_name = :column", Map.of("schema", schema, "table", table, "column", pk), (rs, rowNum) -> rs.getString(1));
        if (!constraints.isEmpty()) {
            exists = jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_catalog.pg_constraint WHERE conname IN (:constraints) AND contype = 'u')", Map.of("constraints", constraints), Boolean.class);
        }
        if (!exists) {
            String constraintName = String.format("%s_%s_%s_unique", schema, table, pk);
            String q = String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE(%s)", schemaTable, constraintName, addDoubleQuotes(pk));
            jdbcTemplate.getJdbcTemplate().execute(q);
        }
    }

    private void createTriggerOnInsertOrUpdate(String schemaTable, String schema, String table) {
        String triggerName = schema + "_" + table + "_internal";
        boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_trigger WHERE NOT tgisinternal AND tgname = :tgname)", Map.of("tgname", triggerName), Boolean.class);
        if (!exists) {
            String q = String.format("CREATE TRIGGER %s BEFORE INSERT OR UPDATE ON %s FOR EACH ROW EXECUTE PROCEDURE %s;", triggerName, schemaTable, INTERNAL_FUNCTION);
            jdbcTemplate.getJdbcTemplate().execute(q);
        }
    }

    private void createOrReplaceLocalRowStateUpdateFunction() {
        String q = String.format(LOCAL_ROW_STATE_UPDATE_FUNC, INTERNAL_FUNCTION, RDM_SYNC_INTERNAL_STATE_COLUMN, RdmSyncLocalRowState.DIRTY);
        jdbcTemplate.getJdbcTemplate().execute(q);
    }

    private void addInternalLocalRowStateColumnIfNotExists(String schemaTable) {
        String q = String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s VARCHAR", schemaTable, RDM_SYNC_INTERNAL_STATE_COLUMN);
        jdbcTemplate.getJdbcTemplate().execute(q);
    }

}
