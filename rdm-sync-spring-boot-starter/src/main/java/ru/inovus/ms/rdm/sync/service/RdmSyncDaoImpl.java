package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public class RdmSyncDaoImpl implements RdmSyncDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<VersionMapping> getVersionMappings() {
        return jdbcTemplate.query("select id,code,version,publication_dt,sys_table,unique_rdm_field,deleted_field from rdm_sync.version",
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
        List<VersionMapping> list = jdbcTemplate.query("select id,code,version,publication_dt,sys_table,unique_rdm_field,deleted_field from rdm_sync.version where code=?",
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
        return jdbcTemplate.query("select sys_field, sys_data_type, rdm_field, rdm_data_type from rdm_sync.field_mapping where code=?",
                (rs, rowNum) -> new FieldMapping(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                ), refbookCode);
    }

    @Override
    public List<Object> getDataIds(String table, String primaryField, String isDeletedField) {
        return jdbcTemplate.query(String.format("select %s from %s where %s is null or %s=false",
                addDoubleQuotes(primaryField), table, addDoubleQuotes(isDeletedField), addDoubleQuotes(isDeletedField)),
                new SingleColumnRowMapper<>());
    }

    @Override
    public void updateVersionMapping(Integer id, String version, LocalDateTime publishDate) {
        jdbcTemplate.update("update rdm_sync.version set version=?, publication_dt=?, update_dt=? where id=?",
                version, publishDate, new Date(), id);
    }

    public void insertRow(String table, LinkedHashMap<String, Object> row) {
        String keys = row.keySet().stream().map(this::addDoubleQuotes).collect(Collectors.joining(","));
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() == null) {
                values.add("null");
            } else {
                values.add("?");
            }
        }
        jdbcTemplate.update(String.format("insert into %s (%s) values(%s)", table, keys, String.join(",", values)),
                row.values().toArray());
    }

    public void updateRow(String table, String primaryField, String isDeletedField, LinkedHashMap<String, Object> row) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                keys.add(addDoubleQuotes(field) + " = null");
            } else {
                keys.add(addDoubleQuotes(field) + " = ?");
            }
        }
        List<Object> values = new ArrayList<>(row.values());
        values.add(row.get(primaryField));
        jdbcTemplate.update(String.format("update %s set %s where %s=? and (%s is null or %s=false)",
                table, String.join(",", keys), addDoubleQuotes(primaryField), addDoubleQuotes(isDeletedField), addDoubleQuotes(isDeletedField)),
                values.toArray());
    }

    @Override
    public void markDeleted(String table, String primaryField, String isDeletedField, Object primaryValue) {
        jdbcTemplate.update(String.format("update %s set %s=true where %s=? and (%s is null or %s=false)", table,
                addDoubleQuotes(isDeletedField), addDoubleQuotes(primaryField), addDoubleQuotes(isDeletedField), addDoubleQuotes(isDeletedField)),
                primaryValue
        );
    }

    private String addDoubleQuotes(String value) {
        return "\"" + value + "\"";
    }
}
