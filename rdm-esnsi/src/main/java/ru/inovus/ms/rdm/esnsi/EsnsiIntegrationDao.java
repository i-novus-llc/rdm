package ru.inovus.ms.rdm.esnsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

class EsnsiIntegrationDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    Integer getLastVersionRevision(String code) {
        final boolean[] contains = new boolean[1];
        Integer revision = jdbcTemplate.queryForObject(
            "SELECT code, revision FROM esnsi_sync.version WHERE code = ?1",
            new Object[]{code},
            (rs, rowNum) -> {contains[0] = true; return rs.getInt(2);}
        );
        if (!contains[0]) {
            jdbcTemplate.update("INSERT INTO esnsi_sync.version VALUES (?1, NULL, NULL)", code);
        }
        return revision;
    }

    @Transactional
    void createEsnsiVersionDataTable(GetClassifierStructureResponseType struct) {
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
    void insert(List<Object[]> batch, GetClassifierStructureResponseType struct) {
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
    void updateLastDownloaded(GetClassifierStructureResponseType struct, Timestamp time) {
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
            throw new RdmException(e);
        }
        String q = "INSERT INTO esnsi_sync.version (code, revision, struct, last_updated) VALUES (?1, ?2, ?3, ?4) " +
                "ON CONFLICT (code) DO UPDATE SET revision = ?2, struct = ?3, last_updated = ?4;";
        jdbcTemplate.update(q, code, revision, structRaw, time);
    }

}
