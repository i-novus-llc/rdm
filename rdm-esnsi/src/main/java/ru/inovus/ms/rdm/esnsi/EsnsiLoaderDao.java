package ru.inovus.ms.rdm.esnsi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage.NONE;

@Component
@Repository
public class EsnsiLoaderDao {

    public static final String DB_REVISION_FIELD_NAME = "revision";
    public static final String FIELD_PREFIX = "field_";

    private static final Logger logger = LoggerFactory.getLogger(EsnsiLoaderDao.class);

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional
    public Integer getLastVersionRevision(String code) {
        return namedParameterJdbcTemplate.query(
            "SELECT revision FROM esnsi_sync.version WHERE code = :code",
            Map.of("code", code),
            (rs, rowNum) -> rs.getInt(1)
        ).stream().findAny().orElse(null);
    }

    @Transactional
    public ClassifierProcessingStage getClassifierProcessingStage(String code) {
        return namedParameterJdbcTemplate.query(
            "SELECT stage FROM esnsi_sync.version WHERE code = :code",
            Map.of("code", code),
            (rs, rowNum) -> rs.getString(1)
        ).stream().findAny().map(ClassifierProcessingStage::valueOf).orElse(null);
    }

    @Transactional
    public void createClassifierProcessingStage(String code) {
        namedParameterJdbcTemplate.update(
            "INSERT INTO esnsi_sync.version (code, stage) VALUES (:code, '" + NONE + "')",
            Map.of("code", code)
        );
    }

    @Transactional
    public void setClassifierProcessingStage(String code, ClassifierProcessingStage stage) {
        Map<String, ?> args = Map.of("code", code, "stage", stage.name());
        namedParameterJdbcTemplate.update("UPDATE esnsi_sync.version SET stage = :stage WHERE code = :code", args);
    }

    @Transactional
    public void dropClassifierDataTablesByWildcard(String wildcard) {
        List<String> queries = namedParameterJdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema='esnsi_data' AND table_type='BASE TABLE' AND table_name LIKE :wildcard",
                Map.of("wildcard", wildcard),
                (rs, rowNum) -> rs.getString(1)
        ).stream().map(tableName -> "DROP TABLE esnsi_data.\"" + tableName + "\"").collect(toList());
        for (String q : queries) {
            try {
                namedParameterJdbcTemplate.getJdbcTemplate().execute(q);
            } catch (DataAccessException ex) {
                logger.error("Can't drop tables previously associated with this classifier. Please do this manually.", ex);
            }
        }
    }

    @Transactional
    @SuppressWarnings("squid:S2077")
    public void createClassifierRevisionDataTable(String tableName, int numFields) {
        String q = "CREATE TABLE esnsi_data.\"" + tableName + "\"(" +
            IntStream.rangeClosed(1, numFields).mapToObj(i -> FIELD_PREFIX + i + " VARCHAR ").collect(joining(", ")) +
        ")";
        namedParameterJdbcTemplate.getJdbcTemplate().execute(q);
    }

    @Transactional
    public void saveClassifierRevisionStruct(String code, int revision, String struct) {
        String q = "INSERT INTO esnsi_sync.struct (code, revision, struct) VALUES (:code, :revision, :struct) ON CONFLICT (code, revision) DO UPDATE SET struct = :struct;";
        namedParameterJdbcTemplate.update(q, Map.of("code", code, DB_REVISION_FIELD_NAME, revision, "struct", struct));
    }

    @Transactional
    public void insertClassifierData(Map<String, String>[] batch, String tableName) {
        String q = "INSERT INTO esnsi_data.\"" + tableName + "\" (" +
            IntStream.rangeClosed(1, batch[0].size()).mapToObj(i -> FIELD_PREFIX + i).collect(joining(", ")) +
        ") VALUES (" +
            IntStream.rangeClosed(1, batch[0].size()).mapToObj(i -> ":field_" + i).collect(joining(", ")) +
        ")";
        namedParameterJdbcTemplate.batchUpdate(q, batch);
    }

    @Transactional
    public boolean isPageProcessorFinished(String pageProcessorId) {
        return namedParameterJdbcTemplate.query(
            "SELECT finished from esnsi_sync.page_processor_state WHERE id = :id",
            Map.of("id", pageProcessorId),
            (rs, rowNum) -> rs.getBoolean(1)
        ).stream().findFirst().orElse(false);
    }

    @Transactional
    public void incrementPageProcessorSeed(String pageProcessorId) {
        namedParameterJdbcTemplate.update(
            "UPDATE esnsi_sync.page_processor_state SET seed = (seed + 1) WHERE id = :id",
            Map.of("id", pageProcessorId)
        );
    }

    @Transactional
    public void setPageProcessorFinished(String pageProcessorId, boolean finished) {
        namedParameterJdbcTemplate.update(
            "UPDATE esnsi_sync.page_processor_state SET finished = :finished WHERE id = :id",
            Map.of("id", pageProcessorId, "finished", finished)
        );
    }

    @Transactional
    public void setClassifierRevisionAndLastUpdatedTimestamp(String code, Integer revision, Timestamp timestamp) {
        Map<String, Object> args;
        if (revision == null || timestamp == null) {
            args = new HashMap<>();
            args.put("code", code);
            args.put(DB_REVISION_FIELD_NAME, revision);
            args.put("timestamp", timestamp);
        } else
            args = Map.of("code", code, DB_REVISION_FIELD_NAME, revision, "timestamp", timestamp);
        namedParameterJdbcTemplate.update(
            "UPDATE esnsi_sync.version SET last_updated = :timestamp, revision = :revision WHERE code = :code",
            args
        );
    }

    @Transactional
    public String getClassifierStruct(String code, int revision) {
        return namedParameterJdbcTemplate.query(
                "SELECT struct FROM esnsi_sync.struct WHERE code = :code AND revision = :revision",
                Map.of("code", code, DB_REVISION_FIELD_NAME, revision),
                (rs, rowNum) -> rs.getString(1)
        ).stream().findFirst().orElse(null);
    }

    @Transactional
    public List<Map<String, Object>> getClassifierData(String tableName, int primaryKeySerialNumber, String lastSeenId, int pageSize) {
        String fieldName = FIELD_PREFIX + primaryKeySerialNumber;
        return namedParameterJdbcTemplate.query(
            "SELECT * FROM esnsi_data.\"" + tableName + "\" WHERE " + fieldName + " > :lastSeenId ORDER BY " + fieldName + " FETCH FIRST " + pageSize + " ROWS ONLY",
            Map.of("lastSeenId", lastSeenId),
            (rs, rowNum) -> {
                Map<String, Object> ans = new HashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
                    ans.put(FIELD_PREFIX + (i), rs.getString(i));
                return ans;
            }
        );
    }

    @Transactional
    public void createPageProcessorStateRecordsResetToDefaultsOnConflict(String pageProcessorIdPattern, int numPageProcessors) {
        Map<String, String>[] batch = new Map[numPageProcessors];
        for (int i = 1; i <= numPageProcessors; i++)
            batch[i - 1] = Map.of("id", pageProcessorIdPattern + i);
        namedParameterJdbcTemplate.batchUpdate(
            "INSERT INTO esnsi_sync.page_processor_state (id, finished, seed) VALUES (:id, TRUE, 0) ON CONFLICT (id) DO UPDATE SET finished = TRUE, seed = 0",
            batch
        );
    }

    @Transactional
    public List<PageProcessor> getFinishedPageProcessors(String pageProcessorWildcard) {
        return namedParameterJdbcTemplate.query(
            "SELECT id, seed FROM esnsi_sync.page_processor_state WHERE id LIKE :pageProcessorWildcard AND finished = TRUE",
            Map.of("pageProcessorWildcard", pageProcessorWildcard),
            (rs, rowNum) -> new PageProcessor(rs.getString(1), rs.getInt(2))
        );
    }

    @Transactional
    @SuppressWarnings("squid:S2077")
    public void createIndexOnClassifierRevisionDataTable(String tableName, int primaryKeySerialNumber) {
        String fieldName = FIELD_PREFIX + primaryKeySerialNumber;
        String idxName = tableName + "_idx";
        namedParameterJdbcTemplate.getJdbcTemplate().execute("CREATE INDEX \"" + idxName + "\" ON esnsi_data.\"" + tableName + "\" USING BTREE (" + fieldName + ")");
    }

    @Transactional
    public boolean lockStage(String classifierCode) {
        try {
            namedParameterJdbcTemplate.getJdbcTemplate().queryForObject("SELECT 1 FROM esnsi_sync.version WHERE code = ? FOR UPDATE", new Object[]{classifierCode}, Integer.class);
            return true;
        } catch (CannotAcquireLockException ex) {
            logger.info("Lock for classifier {} stage is already acquired.", classifierCode, ex);
            return false;
        }
    }

}
