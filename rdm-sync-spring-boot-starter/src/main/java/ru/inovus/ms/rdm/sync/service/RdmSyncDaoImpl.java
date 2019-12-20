package ru.inovus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.api.util.StringUtils.addDoubleQuotes;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public class RdmSyncDaoImpl implements RdmSyncDao {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncDaoImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
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
                ));
    }

    @Override
    public VersionMapping getVersionMapping(String refbookCode) {
        List<VersionMapping> list = jdbcTemplate.query("select id,code,version,publication_dt,sys_table,unique_sys_field,deleted_field,mapping_last_updated,update_dt from rdm_sync.version where code=?",
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
                ), refbookCode);
        return !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public int getLastVersion(String refbookCode) {

        List<Integer> list = jdbcTemplate.query("select mapping_version from rdm_sync.version where code=?",
                (rs, rowNum) -> rs.getInt(1), refbookCode);
        return !list.isEmpty() ? list.get(0) : 0;
    }

    @Override
    public List<FieldMapping> getFieldMapping(String refbookCode) {
        return jdbcTemplate.query("select sys_field, sys_data_type, rdm_field from rdm_sync.field_mapping where code=?",
                (rs, rowNum) -> new FieldMapping(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3)
                ), refbookCode);
    }

    @Override
    public List<Pair<String, String>> getColumnNameAndDataTypeFromLocalDataTable(String schemaTable) {
        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];
        String q = "SELECT FROM information_schema.columns WHERE table_schema = :schema AND table_name = :table";
        List<Pair<String, String>> list = namedParameterJdbcTemplate.query(q, Map.of("schema", schema, "table", table), (rs, rowNum) -> Pair.of(rs.getString(1), rs.getString(2)));
        if (list.isEmpty())
            throw new RdmException("No table '" + table + "' in schema '" + schema + "'.");
        return list;
    }

    @Override
    public List<Object> getDataIds(String table, FieldMapping primaryField) {
        DataTypeEnum dataType = DataTypeEnum.getByDataType(primaryField.getSysDataType());
        return jdbcTemplate.query(String.format("select %s from %s", addDoubleQuotes(primaryField.getSysField()), table),
                (rs, rowNum) -> rdmMappingService.map(FieldType.STRING, dataType, rs.getObject(1)));
    }

    @Override
    public boolean isIdExists(String table, String primaryField, Object primaryValue) {
        return jdbcTemplate.queryForObject(String.format("select count(*)>0 from %s where %s=?", table, addDoubleQuotes(primaryField)),
                Boolean.class,
                primaryValue);
    }

    @Override
    public void updateVersionMapping(Integer id, String version, LocalDateTime publishDate) {
        jdbcTemplate.update("update rdm_sync.version set version=?, publication_dt=?, update_dt=? where id=?",
                version, publishDate, Timestamp.valueOf(LocalDateTime.now(Clock.systemUTC())), id);
    }

    @Override
    public void insertRow(String table, Map<String, Object> row) {
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
        jdbcTemplate.update(String.format("insert into %s (%s) values(%s)", table, keys, String.join(",", values)),
                data.toArray());
    }

    @Override
    public void insertRows(String table, Map<String, Object>[] rows) {
        List<Pair<String, String>> schema = getColumnNameAndDataTypeFromLocalDataTable(table);
        String columnsJoined = schema.stream().map(Pair::getFirst).collect(Collectors.joining(", "));
        String columnsPlaceholdersJoined = schema.stream().map(pair -> ":" + pair.getFirst()).collect(Collectors.joining(", "));
        String q = "INSERT INTO " + table + "(" + columnsJoined + ") VALUES (" + columnsPlaceholdersJoined + ")";
        namedParameterJdbcTemplate.batchUpdate(q, rows);
    }

    public void updateRow(String table, String primaryField, String isDeletedField, Map<String, Object> row) {
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
        jdbcTemplate.update(String.format("update %s set %s where %s=?",
                table, String.join(",", keys), addDoubleQuotes(primaryField)),
                data.toArray());
    }

    @Override
    public void markDeleted(String table, String primaryField, String isDeletedField, Object primaryValue, boolean deleted) {
        jdbcTemplate.update(String.format("update %s set %s=? where %s=?", table, addDoubleQuotes(isDeletedField), addDoubleQuotes(primaryField)),
                deleted,
                primaryValue
        );
    }

    @Override
    public void markDeleted(String table, String isDeletedField, boolean deleted) {
        jdbcTemplate.update(String.format("update %s set %s=?", table, addDoubleQuotes(isDeletedField)),
                deleted
        );
    }

    @Override
    public void log(String status, String refbookCode, String oldVersion, String newVersion, String message, String stack) {
        jdbcTemplate.update("insert into rdm_sync.log (code, current_version, new_version, status, date, message, stack) values(?,?,?,?,?,?,?)",
                refbookCode, oldVersion, newVersion, status, new Date(), message, stack);
    }

    @Override
    public List<Log> getList(LocalDate date, String refbookCode) {
        LocalDate end = date.plusDays(1);
        List<Object> args = new ArrayList<>();
        args.add(date);
        args.add(end);
        if (refbookCode != null)
            args.add(refbookCode);
        return jdbcTemplate.query(
                String.format("select id, code, current_version, new_version, status, date, message, stack from rdm_sync.log where date>=? and date<? %s", refbookCode != null ? "and code=?" : ""),
                (rs, rowNum) -> new Log(rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getTimestamp(6).toLocalDateTime(),
                        rs.getString(7),
                        rs.getString(8)),
                args.toArray());
    }

    @Override
    public void upsertVersionMapping(XmlMappingRefBook versionMapping) {
        jdbcTemplate.update("insert into rdm_sync.version(code, sys_table, unique_sys_field, deleted_field, mapping_version) values (?, ?, ?, ?, ? )" +
                        "            on conflict (code)\n" +
                        "            do update set (sys_table, unique_sys_field, deleted_field, mapping_version) = (?, ?, ?, ?)",
                versionMapping.getCode(), versionMapping.getSysTable(), versionMapping.getUniqueSysField(), versionMapping.getDeletedField(), versionMapping.getMappingVersion(),
                versionMapping.getSysTable(), versionMapping.getUniqueSysField(), versionMapping.getDeletedField(), versionMapping.getMappingVersion()
        );
    }

    @Override
    public void insertFieldMapping(String code, List<XmlMappingField> fieldMappings) {

        jdbcTemplate.update("delete from rdm_sync.field_mapping where code = ?", code);
        jdbcTemplate.batchUpdate(
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
            jdbcTemplate.queryForObject("SELECT 1 FROM rdm_sync.version WHERE code = ? FOR UPDATE NOWAIT", new Object[] {code}, Integer.class);
            return true;
        } catch (CannotAcquireLockException ex) {
            logger.info("Lock for refbook {} is already acquired.", code, ex);
            return false;
        }
    }

}
