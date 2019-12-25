package ru.inovus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.util.StringUtils;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.Log;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingRefBook;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.api.util.StringUtils.addDoubleQuotes;
import static ru.inovus.ms.rdm.api.util.StringUtils.addSingleQuotes;
import static ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState.RDM_SYNC_INTERNAL_STATE_COLUMN;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public class RdmSyncDaoImpl implements RdmSyncDao {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncDaoImpl.class);

    private static final String INTERNAL_FUNCTION = "rdm_sync_internal_update_local_row_state()";
    private static final String LOCAL_ROW_STATE_UPDATE_FUNC =
    "CREATE OR REPLACE FUNCTION\n" +
    "   %1$s\n" +
    "RETURNS TRIGGER AS\n" +
    "   $$\n" +
    "       BEGIN\n" +
    "			NEW.%2$s='%3$s';\n" +
    "           RETURN NEW;\n" +
    "       END;\n" +
    "   $$\n" +
    "LANGUAGE 'plpgsql'";

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private RdmMappingService rdmMappingService;

    @Override
    public List<VersionMapping> getVersionMappings() {
        return jdbcTemplate.query("select id,code,version,publication_dt,sys_table,unique_sys_field,deleted_field,mapping_last_updated,update_dt from rdm_sync.version",
            (rs, rowNum) -> new VersionMapping(
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3),
                rs.getTimestamp(4) != null ? rs.getTimestamp(4).toLocalDateTime() : null,
                rs.getString(5),
                rs.getString(6),
                rs.getString(7),
                rs.getTimestamp(8) == null ? LocalDateTime.MIN : rs.getTimestamp(8).toLocalDateTime(),
                rs.getTimestamp(9) == null ? LocalDateTime.MIN : rs.getTimestamp(9).toLocalDateTime()
            )
        );
    }

    @Override
    public VersionMapping getVersionMapping(String refbookCode) {
        List<VersionMapping> list = jdbcTemplate.query(
            "select id,code,version,publication_dt,sys_table,unique_sys_field,deleted_field,mapping_last_updated,update_dt from rdm_sync.version where code=:code",
            Map.of("code", refbookCode),
            (rs, rowNum) -> new VersionMapping(
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3),
                rs.getTimestamp(4) != null ? rs.getTimestamp(4).toLocalDateTime() : null,
                rs.getString(5),
                rs.getString(6),
                rs.getString(7),
                rs.getTimestamp(8) == null ? LocalDateTime.MIN : rs.getTimestamp(8).toLocalDateTime(),
                rs.getTimestamp(9) == null ? LocalDateTime.MIN : rs.getTimestamp(9).toLocalDateTime()
            )
        );
        return !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public int getLastVersion(String refbookCode) {
        List<Integer> list = jdbcTemplate.query("select mapping_version from rdm_sync.version where code=:code",
            Map.of("code", refbookCode),
            (rs, rowNum) -> rs.getInt(1)
        );
        return !list.isEmpty() ? list.get(0) : 0;
    }

    @Override
    public List<FieldMapping> getFieldMapping(String refbookCode) {
        return jdbcTemplate.query("select sys_field, sys_data_type, rdm_field from rdm_sync.field_mapping where code=:code",
            Map.of("code", refbookCode),
            (rs, rowNum) -> new FieldMapping(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3)
            )
        );
    }

    @Override
    public List<Pair<String, String>> getColumnNameAndDataTypeFromLocalDataTable(String schemaTable) {
        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];
        String q = "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = :schema AND table_name = :table AND column_name != :internal_local_row_state_column";
        List<Pair<String, String>> list = jdbcTemplate.query(q, Map.of("schema", schema, "table", table, "internal_local_row_state_column", RdmSyncLocalRowState.RDM_SYNC_INTERNAL_STATE_COLUMN), (rs, rowNum) -> Pair.of(rs.getString(1), rs.getString(2)));
        if (list.isEmpty())
            throw new RdmException("No table '" + table + "' in schema '" + schema + "'.");
        return list;
    }

    @Override
    public List<Object> getDataIds(String table, FieldMapping primaryField) {
        DataTypeEnum dataType = DataTypeEnum.getByDataType(primaryField.getSysDataType());
        return jdbcTemplate.query(
            String.format("select %s from %s", addDoubleQuotes(primaryField.getSysField()), table),
            (rs, rowNum) -> rdmMappingService.map(FieldType.STRING, dataType, rs.getObject(1))
        );
    }

    @Override
    public boolean isIdExists(String table, String primaryField, Object primaryValue) {
        return jdbcTemplate.queryForObject(
            String.format("select count(*)>0 from %s where %s=:primary", table, addDoubleQuotes(primaryField)),
            Map.of("primary", primaryValue),
            Boolean.class
        );
    }

    @Override
    public void updateVersionMapping(Integer id, String version, LocalDateTime publishDate) {
        jdbcTemplate.update(
            "update rdm_sync.version set version=:version, publication_dt=:publication_dt, update_dt=:update_dt where id=:id",
            Map.of("version", version, "publication_dt", publishDate, "update_dt", LocalDateTime.now(Clock.systemUTC()), "id", id)
        );
    }

    @Override
    public void insertRow(String table, Map<String, Object> row, boolean markSynced) {
        String keys = row.keySet().stream().map(StringUtils::addDoubleQuotes).collect(Collectors.joining(","));
        List<String> values = new ArrayList<>();
        List<Object> data = new ArrayList<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() == null) {
                values.add("null");
            } else {
                values.add("?");
                data.add(entry.getValue());
            }
        }
        if (markSynced) {
            keys += ", " + addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN);
            values.add(addSingleQuotes(RdmSyncLocalRowState.SYNCED.name()));
        }
        jdbcTemplate.getJdbcTemplate().update(
            String.format("insert into %s (%s) values(%s)", table, keys, String.join(",", values)),
            data.toArray()
        );
    }

    @Override
    public void updateRow(String table, String primaryField, String isDeletedField, Map<String, Object> row, boolean markSynced) {
        List<String> keys = new ArrayList<>();
        List<Object> data = new ArrayList<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String field = entry.getKey();
            if (entry.getValue() == null) {
                keys.add(addDoubleQuotes(field) + " = null");
            } else {
                keys.add(addDoubleQuotes(field) + " = ?");
                data.add(entry.getValue());
            }
        }
        data.add(row.get(primaryField));
        if (markSynced) {
            keys.add(addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN));
            data.add(RdmSyncLocalRowState.SYNCED.name());
        }
        jdbcTemplate.getJdbcTemplate().update(
            String.format("update %s set %s where %s=?",table, String.join(",", keys), addDoubleQuotes(primaryField)),
            data.toArray()
        );
    }

    @Override
    public void markDeleted(String table, String primaryField, String isDeletedField, Object primaryValue, boolean deleted, boolean markSynced) {
        if (markSynced) {
            jdbcTemplate.getJdbcTemplate().update(
                String.format("update %s set %s=?, %s=? where %s=?", table, addDoubleQuotes(isDeletedField), addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN), addDoubleQuotes(primaryField)),
                deleted,
                RdmSyncLocalRowState.SYNCED.name(),
                primaryValue
            );
        } else {
            jdbcTemplate.getJdbcTemplate().update(
                String.format("update %s set %s=? where %s=?", table, addDoubleQuotes(isDeletedField), addDoubleQuotes(primaryField)),
                deleted,
                primaryValue
            );
        }
    }

    @Override
    public void markDeleted(String table, String isDeletedField, boolean deleted, boolean markSynced) {
        if (markSynced) {
            jdbcTemplate.getJdbcTemplate().update(
                String.format("update %s set %s=?, %s=?", table, addDoubleQuotes(isDeletedField), addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN)),
                deleted,
                RdmSyncLocalRowState.SYNCED.name()
            );
        } else {
            jdbcTemplate.getJdbcTemplate().update(
                String.format("update %s set %s=?", table, addDoubleQuotes(isDeletedField)),
                deleted
            );
        }
    }

    @Override
    public void log(String status, String refbookCode, String oldVersion, String newVersion, String message, String stack) {
        jdbcTemplate.getJdbcTemplate().update(
            "insert into rdm_sync.log (code, current_version, new_version, status, date, message, stack) values(?,?,?,?,?,?,?)",
            refbookCode, oldVersion, newVersion, status, new Date(), message, stack
        );
    }

    @Override
    public List<Log> getList(LocalDate date, String refbookCode) {
        LocalDate end = date.plusDays(1);
        List<Object> args = new ArrayList<>();
        args.add(date);
        args.add(end);
        if (refbookCode != null)
            args.add(refbookCode);
        return jdbcTemplate.getJdbcTemplate().query(
            String.format("select id, code, current_version, new_version, status, date, message, stack from rdm_sync.log where date>=? and date<? %s", refbookCode != null ? "and code=?" : ""),
            (rs, rowNum) -> new Log(rs.getLong(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getTimestamp(6).toLocalDateTime(),
                rs.getString(7),
                rs.getString(8)),
            args.toArray()
        );
    }

    @Override
    public void upsertVersionMapping(XmlMappingRefBook versionMapping) {
        jdbcTemplate.getJdbcTemplate().update("insert into rdm_sync.version(code, sys_table, unique_sys_field, deleted_field, mapping_version) values (?, ?, ?, ?, ? )" +
                        "            on conflict (code)\n" +
                        "            do update set (sys_table, unique_sys_field, deleted_field, mapping_version) = (?, ?, ?, ?)",
                versionMapping.getCode(), versionMapping.getSysTable(), versionMapping.getUniqueSysField(), versionMapping.getDeletedField(), versionMapping.getMappingVersion(),
                versionMapping.getSysTable(), versionMapping.getUniqueSysField(), versionMapping.getDeletedField(), versionMapping.getMappingVersion()
        );
    }

    @Override
    public void insertFieldMapping(String code, List<XmlMappingField> fieldMappings) {

        jdbcTemplate.getJdbcTemplate().update("delete from rdm_sync.field_mapping where code = ?", code);
        jdbcTemplate.getJdbcTemplate().batchUpdate(
                "insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field) " +
                        "values (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {

                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, code);
                        ps.setString(2, fieldMappings.get(i).getSysField());
                        ps.setString(3, fieldMappings.get(i).getSysDataType());
                        ps.setString(4, fieldMappings.get(i).getRdmField());
                    }

                    public int getBatchSize() {
                        return fieldMappings.size();
                    }

                });
    }

    @Override
    public boolean lockRefbookForUpdate(String code) {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM rdm_sync.version WHERE code = :code FOR UPDATE NOWAIT", Map.of("code", code), Integer.class);
            logger.info("Lock for refbook {} successfully acquired.", code);
            return true;
        } catch (CannotAcquireLockException ex) {
            logger.info("Lock for refbook {} cannot be acquired.", code, ex);
            return false;
        }
    }

    @Override
    public void addInternalLocalRowStateUpdateTrigger(String schema, String table) {
        String triggerName = getInternalLocalStateUpdateTriggerName(schema, table);
        String schemaTable = schema + "." + table;
        boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_trigger WHERE NOT tgisinternal AND tgname = :tgname)", Map.of("tgname", triggerName), Boolean.class);
        if (!exists) {
            String q = String.format("CREATE TRIGGER %s BEFORE INSERT OR UPDATE ON %s FOR EACH ROW EXECUTE PROCEDURE %s;", triggerName, schemaTable, INTERNAL_FUNCTION);
            jdbcTemplate.getJdbcTemplate().execute(q);
        }
    }

    @Override
    public void createOrReplaceLocalRowStateUpdateFunction() {
        String q = String.format(LOCAL_ROW_STATE_UPDATE_FUNC, INTERNAL_FUNCTION, RDM_SYNC_INTERNAL_STATE_COLUMN, RdmSyncLocalRowState.DIRTY);
        jdbcTemplate.getJdbcTemplate().execute(q);
    }

    @Override
    public void addInternalLocalRowStateColumnIfNotExists(String schema, String table) {
        String schemaTable = schema + "." + table;
        boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = :schema AND table_name = :table AND column_name = :internal_state_column)", Map.of("schema", schema, "table", table, "internal_state_column", RDM_SYNC_INTERNAL_STATE_COLUMN), Boolean.class);
        if (!exists) {
            String q = String.format("ALTER TABLE %s ADD COLUMN %s VARCHAR NOT NULL DEFAULT '%s'", schemaTable, RDM_SYNC_INTERNAL_STATE_COLUMN, RdmSyncLocalRowState.DIRTY);
            jdbcTemplate.getJdbcTemplate().execute(q);
            q = String.format("CREATE INDEX ON %s (%s)", schemaTable, addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN));
            jdbcTemplate.getJdbcTemplate().execute(q);
            int n = jdbcTemplate.update(String.format("UPDATE %s SET %s = :synced", schemaTable, addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN)), Map.of("synced", RdmSyncLocalRowState.SYNCED.name()));
            if (n != 0)
                logger.info("{} records updated internal state to {} in table {}", n, RdmSyncLocalRowState.SYNCED, schemaTable);
        }
    }

    @Override
    public void disableInternalLocalRowStateUpdateTrigger(String table) {
        String[] split = table.split("\\.");
        String q = String.format("ALTER TABLE %s DISABLE TRIGGER %s", table, getInternalLocalStateUpdateTriggerName(split[0], split[1]));
        jdbcTemplate.getJdbcTemplate().execute(q);
    }

    @Override
    public void enableInternalLocalRowStateUpdateTrigger(String table) {
        String[] split = table.split("\\.");
        String q = String.format("ALTER TABLE %s ENABLE TRIGGER %s", table, getInternalLocalStateUpdateTriggerName(split[0], split[1]));
        jdbcTemplate.getJdbcTemplate().execute(q);
    }

    @Override
    public List<HashMap<String, Object>> getRecordsOfStateWithLimitOffset(String table, int limit, int offset, final boolean keysToCamelCase, RdmSyncLocalRowState state) {
        String q = String.format("SELECT * FROM %s WHERE %s = :state LIMIT %d OFFSET %d", table, addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN), limit, offset);
        var v = new Object() {
            int n = -1;
        };
        return jdbcTemplate.query(q, Map.of("state", state.name()), (rs, rowNum) -> {
            HashMap<String, Object> map = new HashMap<>();
            if (v.n == -1)
                v.n = getInternalStateColumnIdx(rs.getMetaData(), table);
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                if (i != v.n) {
                    Object val = rs.getObject(i);
                    String key = keysToCamelCase ? StringUtils.snakeCaseToCamelCase(rs.getMetaData().getColumnName(i)) : rs.getMetaData().getColumnName(i);
                    map.put(key, val);
                }
            }
            return map;
        });
    }

    private int getInternalStateColumnIdx(ResultSetMetaData meta, String table) throws SQLException {
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (meta.getColumnName(i).equals(RDM_SYNC_INTERNAL_STATE_COLUMN)) {
                return i;
            }
        }
        throw new SQLException("Internal state \"" + RDM_SYNC_INTERNAL_STATE_COLUMN + "\" column not found in " + table);
    }

    @Override
    public <T> boolean setLocalRecordsState(String table, String pk, List<? extends T> pvs, RdmSyncLocalRowState expectedState, RdmSyncLocalRowState toState) {
        if (pvs.isEmpty())
            return false;
        String q = String.format("SELECT COUNT(*) FROM %s WHERE %s IN (:pvs)", table, addDoubleQuotes(pk));
        int count = jdbcTemplate.queryForObject(q, Map.of("pvs", pvs), Integer.class);
        if (count == 0)
            return false;
        q = String.format("UPDATE %1$s SET %2$s = :toState WHERE %3$s IN (:pvs) AND %2$s = :expectedState", table, addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN), addDoubleQuotes(pk));
        int n = jdbcTemplate.update(q, Map.of("toState", toState.name(), "pvs", pvs, "expectedState", expectedState.name()));
        return n == count;
    }

    @Override
    public RdmSyncLocalRowState getLocalRowState(String table, String pk, Object pv) {
        String q = String.format("SELECT %s FROM %s WHERE %s = :pv", addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN), table, addDoubleQuotes(pk));
        List<String> list = jdbcTemplate.query(q, Map.of("pv", pv), (rs, rowNum) -> rs.getString(1));
        if (list.size() > 1)
            throw new RdmException("Cannot identify record by " + pk);
        return list.stream().findAny().map(RdmSyncLocalRowState::valueOf).orElse(null);
    }

    private static String getInternalLocalStateUpdateTriggerName(String schema, String table) {
        return schema + "_" + table + "_intrnl_lcl_rw_stt_updt";
    }

}
