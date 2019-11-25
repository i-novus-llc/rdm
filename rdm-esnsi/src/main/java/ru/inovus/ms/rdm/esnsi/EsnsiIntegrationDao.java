package ru.inovus.ms.rdm.esnsi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.esnsi.api.CnsiResponse;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;
import ru.inovus.ms.rdm.esnsi.api.ObjectFactory;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage.NONE;

@Component
@Repository
public class EsnsiIntegrationDao {

    public static final String DB_REVISION_FIELD_NAME = "revision";

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationDao.class);

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private static final JAXBContext STRUCT_CTX;

    static {
        try {
            STRUCT_CTX = JAXBContext.newInstance(CnsiResponse.class);
        } catch (JAXBException e) {
//          Не выбросится
            throw new EsnsiSyncException(e);
        }
    }

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Transactional
    public Integer getLastVersionRevisionAndCreateNewIfNecessary(String code) {
        Map<String, ?> arg = Map.of("code", code);
        List<Integer> list = namedParameterJdbcTemplate.query(
            "SELECT revision FROM esnsi_sync.version WHERE code = :code",
            arg,
            (rs, rowNum) -> rs.getInt(1)
        );
        if (list.isEmpty()) {
            namedParameterJdbcTemplate.update("INSERT INTO esnsi_sync.version (code, stage) VALUES (:code, '" + NONE + "')", arg);
            return null;
        }
        return list.get(0);
    }

    @Transactional
    public ClassifierProcessingStage getClassifierProcessingStageAndCreateNewIfNecessary(String code) {
        Map<String, ?> arg = Map.of("code", code);
        List<String> list = namedParameterJdbcTemplate.query(
            "SELECT stage FROM esnsi_sync.version WHERE code = :code",
            arg,
            (rs, rowNum) -> rs.getString(1)
        );
        if (list.isEmpty()) {
            namedParameterJdbcTemplate.update("INSERT INTO esnsi_sync.version (code, stage) VALUES (:code, '" + NONE + "')", arg);
            return ClassifierProcessingStage.NONE;
        }
        return ClassifierProcessingStage.valueOf(list.get(0));
    }

    @Transactional
    public void setClassifierProcessingStage(String code, ClassifierProcessingStage stage, Executable exec) {
        Map<String, ?> args = Map.of("code", code, "stage", stage.name());
        namedParameterJdbcTemplate.update("UPDATE esnsi_sync.version SET stage = :stage WHERE code = :code", args);
        try {
            exec.exec();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new EsnsiSyncException(e);
        }
    }

    @Transactional(readOnly = true)
    public ClassifierProcessingStage getClassifierProcessingStage(String code) {
        Map<String, ?> arg = Map.of("code", code);
        return ClassifierProcessingStage.valueOf(
            namedParameterJdbcTemplate.query(
                "SELECT stage FROM esnsi_sync.version WHERE code = :code",
                arg,
                (rs, rowNum) -> rs.getString(1)
            ).iterator().next()
        );
    }

    @Transactional
    public void createEsnsiVersionDataTableAndRemovePreviousIfNecessaryAndSaveStruct(GetClassifierStructureResponseType struct) {
        String code = struct.getClassifierDescriptor().getCode();
        String publicId = struct.getClassifierDescriptor().getPublicId();
        String wildcard = publicId + "-%";
        Map<String, ?> arg = Map.of("wildcard", wildcard);
        List<String> queries = namedParameterJdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema='esnsi_data' AND table_type='BASE TABLE' AND table_name LIKE :wildcard",
                arg,
                (rs, rowNum) -> rs.getString(1)
        ).stream().map(tableName -> "DROP TABLE esnsi_data.\"" + tableName + "\"").collect(toList());
        for (String q : queries) {
            try {
                namedParameterJdbcTemplate.getJdbcTemplate().execute(q);
            } catch (DataAccessException ex) {
                logger.error("Can't drop tables previously associated with this classifier. Please do this manually.", ex);
            }
        }
        int revision = struct.getClassifierDescriptor().getRevision();
        String tableName = getClassifierSpecificDataTableName(publicId, revision);
        String q =
            "CREATE TABLE " +
                tableName + " (" +
                    IntStream.rangeClosed(1, struct.getAttributeList().size()).mapToObj(i -> "field_" + i + " VARCHAR ").collect(joining(", ")) +
                ");";
        namedParameterJdbcTemplate.getJdbcTemplate().execute(q);
        String structRaw;
        try {
            CnsiResponse cnsiResponse = OBJECT_FACTORY.createCnsiResponse();
            cnsiResponse.setGetClassifierStructure(struct);
            StringWriter sw = new StringWriter();
            STRUCT_CTX.createMarshaller().marshal(cnsiResponse, sw);
            structRaw = sw.toString();
        } catch (JAXBException e) {
//          Никогда не выбросится
            throw new EsnsiSyncException(e);
        }
        q = "INSERT INTO esnsi_sync.struct (code, revision, struct) VALUES (:code, :revision, :struct) ON CONFLICT (code, revision) DO UPDATE SET struct = :struct;";
        namedParameterJdbcTemplate.update(q, Map.of("code", code, DB_REVISION_FIELD_NAME, revision, "struct", structRaw));
    }

    @Transactional
    @SuppressWarnings("squid:S2077")
    public void insert(List<Object[]> batch, String tableName, String pageProcessorId, Executable exec) {
        boolean finished = isPageProcessorIdle(pageProcessorId);
        if (!batch.isEmpty() && !finished) {
            String q = "INSERT INTO " + tableName + "(" +
                    IntStream.rangeClosed(1, batch.get(0).length).mapToObj(i -> "field_" + i).collect(joining(", ")) +
                    ") VALUES (" +
                    IntStream.rangeClosed(1, batch.get(0).length).mapToObj(i -> "?").collect(joining(", ")) +
                    ")";
            JdbcOperations jdbcOperations = namedParameterJdbcTemplate.getJdbcTemplate();
            jdbcOperations.batchUpdate(q, batch);
        }
        if (!finished) {
            String q = "UPDATE esnsi_sync.page_processor_state SET finished = TRUE, seed = (seed + 1) WHERE id = :id";
            namedParameterJdbcTemplate.update(q, Map.of("id", pageProcessorId));
        }
        try {
            exec.exec();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new EsnsiSyncException(e);
        }
    }

    @Transactional
    public void updateLastDownloaded(String code, int revision, Timestamp time) {
        String q = "UPDATE esnsi_sync.version SET revision = :revision, last_updated = :time, stage = :stage WHERE code = :code";
        namedParameterJdbcTemplate.update(q, Map.of("code", code, DB_REVISION_FIELD_NAME, revision, "time", time, "stage", NONE.name()));
    }

    @Transactional(readOnly = true)
    public GetClassifierStructureResponseType getStruct(String code, int revision) {
        List<String> s = namedParameterJdbcTemplate.query(
            "SELECT struct FROM esnsi_sync.struct WHERE code = :code AND revision = :revision",
            Map.of("code", code, DB_REVISION_FIELD_NAME, revision),
            (rs, rowNum) -> rs.getString(1)
        );
        if (s.isEmpty())
            return null;
        try {
            return ((CnsiResponse) STRUCT_CTX.createUnmarshaller().unmarshal(new StringReader(s.iterator().next()))).getGetClassifierStructure();
        } catch (JAXBException e) {
            logger.error("Unable to parse classifier structure XML.", e);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public void readRows(Consumer<String[]> consumer, String code, int revision) {
        var ref = new Object() {
            String[] row = null;
        };
        namedParameterJdbcTemplate.query(
            "SELECT * FROM " + getClassifierSpecificDataTableName(code, revision),
            rs -> {
                if (ref.row == null)
                    ref.row = new String[rs.getMetaData().getColumnCount()];
                for (int i = 0; i < ref.row.length; i++)
                    ref.row[i] = rs.getString(i + 1);
                consumer.accept(ref.row);
            }
        );
    }

    @Transactional
    public void createPageProcessorStateRecords(String code, int revision, int batchSize) {
        String baseId = code + "-" + revision + "-";
        Map<String, ?>[] batch = new Map[batchSize];
        for (int i = 0; i < batchSize; i++)
            batch[i] = Map.of("id", baseId + (i + 1));
        String q = "INSERT INTO esnsi_sync.page_processor_state (id, finished, seed) VALUES (:id, TRUE, 0) ON CONFLICT (id) DO UPDATE SET finished = TRUE, seed = 0";
        namedParameterJdbcTemplate.batchUpdate(q, batch);
    }

    @Transactional(readOnly = true)
    public List<PageProcessor> getIdlePageProcessors(String code, int revision) {
        String wildcard = code + "-" + revision + "-%";
        Map<String, String> arg = Map.of("wildcard", wildcard);
        String q = "SELECT id, seed FROM esnsi_sync.page_processor_state WHERE id LIKE :wildcard AND finished = TRUE";
        return namedParameterJdbcTemplate.query(q, arg, (rs, rowNum) -> new PageProcessor(rs.getString(1), rs.getInt(2)));
    }

    @Transactional
    public void setPageProcessorBusy(String pageProcessorId, Executable exec) {
        String q = "UPDATE esnsi_sync.page_processor_state SET finished = FALSE WHERE id = :id";
        namedParameterJdbcTemplate.update(q, Map.of("id", pageProcessorId));
        try {
            exec.exec();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new EsnsiSyncException(e);
        }
    }

    @Transactional(readOnly = true)
    public boolean isPageProcessorIdle(String pageProcessorId) {
        String q = "SELECT finished from esnsi_sync.page_processor_state WHERE id = :id";
        return namedParameterJdbcTemplate.query(q, Map.of("id", pageProcessorId), (rs, rowNum) -> rs.getBoolean(1)).iterator().next();

    }

    public static String getClassifierSpecificDataTableName(String publicId, int revision) {
        return "esnsi_data.\"" + publicId + "-" + revision + "\"";
    }

}
