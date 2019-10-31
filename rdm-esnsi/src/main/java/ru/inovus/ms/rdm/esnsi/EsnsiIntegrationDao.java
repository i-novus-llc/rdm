package ru.inovus.ms.rdm.esnsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

class EsnsiIntegrationDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    Integer getLastVersionRevision(String code) {
        throw new UnsupportedOperationException();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String createEsnsiVersionDataTable(String code, int revision, GetClassifierStructureResponseType struct) {
        throw new UnsupportedOperationException();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void insert(String tableName, List<Map<String, Object>> batch) {
        throw new UnsupportedOperationException();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void updateLastDownloaded(String code, int revision, Timestamp time) {
        throw new UnsupportedOperationException();
    }
}
