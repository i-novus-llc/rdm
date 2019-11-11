package ru.inovus.ms.rdm.esnsi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionListResponseType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

@Component
@Repository
class EsnsiIntegrationDao {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiIntegrationDao.class);

    private static final JAXBContext STRUCT_CTX;

    static {
        try {
            STRUCT_CTX = JAXBContext.newInstance(GetClassifierStructureResponseType.class);
        } catch (JAXBException e) {
//          Не выбросится
            throw new EsnsiSyncException(e);
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public Integer getLastVersionRevisionAndCreateNewIfNecessary(String code) {
        final boolean[] contains = new boolean[1];
        Integer revision = jdbcTemplate.queryForObject(
            "SELECT code, revision FROM esnsi_sync.version WHERE code = ?1",
            (rs, rowNum) -> {contains[0] = true; return rs.getInt(2);},
            code
        );
        if (!contains[0]) {
            jdbcTemplate.update("INSERT INTO esnsi_sync.version VALUES (?1, NULL, NULL)", code);
        }
        return revision;
    }

    @Transactional
    public void createEsnsiVersionDataTable(GetClassifierStructureResponseType struct) {
        String code = struct.getClassifierDescriptor().getCode();
        int revision = struct.getClassifierDescriptor().getRevision();
        String tableName = "esnsi_data.\"" + code + "-" + revision + "\"";
        String q =
            "CREATE TABLE " +
                tableName + " (" +
                    IntStream.rangeClosed(1, struct.getAttributeList().size()).mapToObj(i -> "field_" + i + " VARCHAR ").collect(joining(", ")) +
                ");";
        jdbcTemplate.execute(q);
    }

    @Transactional
    public void insert(List<Object[]> batch, GetClassifierStructureResponseType struct) {
        String code = struct.getClassifierDescriptor().getCode();
        int revision = struct.getClassifierDescriptor().getRevision();
        String tableName = "data.\"" + code + "-" + revision + "\"";
        StringBuilder q = new StringBuilder();
        q.append("INSERT INTO ").append(tableName).append(" (");
        q.append(IntStream.rangeClosed(1, struct.getAttributeList().size()).mapToObj(i -> "field_" + i).collect(joining(", ")));
        q.append(") VALUES (");
        q.append(IntStream.rangeClosed(1, struct.getAttributeList().size()).mapToObj(i -> "?" + i).collect(joining(", ")));
        q.append(")");
        jdbcTemplate.batchUpdate(q.toString(), batch);
    }

    @Transactional
    public void updateLastDownloaded(GetClassifierStructureResponseType struct, Timestamp time) {
        String code = struct.getClassifierDescriptor().getCode();
        int revision = struct.getClassifierDescriptor().getRevision();
        String structRaw;
        try {
            StringWriter sw = new StringWriter();
            JAXBContext jaxbContext = JAXBContext.newInstance(GetClassifierStructureResponseType.class);
            jaxbContext.createMarshaller().marshal(struct, sw);
            structRaw = sw.toString();
        } catch (JAXBException e) {
//          Никогда не выбросится
            throw new EsnsiSyncException(e);
        }
        String q = "INSERT INTO esnsi_sync.version (code, revision, struct, last_updated) VALUES (?1, ?2, ?3, ?4) " +
                "ON CONFLICT (code) DO UPDATE SET revision = ?2, struct = ?3, last_updated = ?4;";
        jdbcTemplate.update(q, code, revision, structRaw, time);
    }

    @Transactional(readOnly = true)
    public GetClassifierRevisionListResponseType getStruct(String code) {
        String s = jdbcTemplate.queryForObject("SELECT struct FROM esnsi_sync.version WHERE code = ?1", String.class, code);
        if (s == null)
            return null;
        try {
            return (GetClassifierRevisionListResponseType) STRUCT_CTX.createUnmarshaller().unmarshal(new StringReader(s));
        } catch (JAXBException e) {
            logger.error("Unable to parse dictionary structure XML.", e);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public void readRows(Consumer<String[]> consumer, String code, int revision) {
        var ref = new Object() {
            String[] row = null;
        };
        jdbcTemplate.query(
            "SELECT * FROM " + getTableName(code, revision),
            rs -> {
                if (ref.row == null)
                    ref.row = new String[rs.getMetaData().getColumnCount()];
                for (int i = 0; i < ref.row.length; i++)
                    ref.row[i] = rs.getString(i + 1);
                consumer.accept(ref.row);
            }
        );
    }

    private String getTableName(String code, int revision) {
        return "data.\"" + code + "-" + revision + "\"";
    }

}
