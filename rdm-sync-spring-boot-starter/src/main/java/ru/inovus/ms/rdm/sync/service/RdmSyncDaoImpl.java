package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.Log;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public class RdmSyncDaoImpl implements RdmSyncDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RdmMappingService rdmMappingService;

    @Override
    public List<VersionMapping> getVersionMappings() {
        return jdbcTemplate.query("select id,code,version,publication_dt,sys_table,unique_sys_field,deleted_field from rdm_sync.version",
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getTimestamp(4) != null ? rs.getTimestamp(4).toLocalDateTime() : null,
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)
                ));
    }

    @Override
    public VersionMapping getVersionMapping(String refbookCode) {
        List<VersionMapping> list = jdbcTemplate.query("select id,code,version,publication_dt,sys_table,unique_sys_field,deleted_field from rdm_sync.version where code=?",
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getTimestamp(4) != null ? rs.getTimestamp(4).toLocalDateTime() : null,
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)
                ), refbookCode);
        return !list.isEmpty() ? list.get(0) : null;
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
                version, publishDate, new Date(), id);
    }

    @Override
    public void insertRow(String table, Map<String, Object> row) {
        String keys = row.keySet().stream().map(this::addDoubleQuotes).collect(Collectors.joining(","));
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

    private String addDoubleQuotes(String value) {
        return "\"" + value + "\"";
    }
}
