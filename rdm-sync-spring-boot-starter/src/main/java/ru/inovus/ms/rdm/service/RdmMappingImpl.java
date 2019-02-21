package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.inovus.ms.rdm.model.FieldMapping;
import ru.inovus.ms.rdm.model.VersionMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public class RdmMappingImpl implements RdmMapping {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void map(VersionMapping versionMapping, List<FieldMapping> fieldMappings, DiffRowValue diffRowValue) {
        Map<String, Object> mappedValues = new HashMap<>();
        for (DiffFieldValue rdmValue : diffRowValue.getValues()) {
            FieldMapping fieldMapping = fieldMappings.stream().filter(f->f.getRdmField().equals(rdmValue.getField().getName())).findAny().orElse(null);
            if (fieldMapping == null){
                //поле не ведется у клиента
                continue;
            }
            Field rdmField = rdmValue.getField();
            //если типы данных бд у полей совпадают, то сохраняем данные без изменений
            if (rdmField.getType().equals(fieldMapping.getSysDataType())){
                mappedValues.put(fieldMapping.getSysField(), rdmValue.getNewValue());
                continue;
            }

        }
    }

    private String getDeletedField(String refbookCode, String version) {
        return jdbcTemplate.queryForObject("select deleted_field from rdm_sync.version where code=? and version=?", String.class, refbookCode, version);
    }

    private List<FieldMapping> getFieldMapping(String code, String version) {
        return jdbcTemplate.query("select sys_field, sys_data_type, rdm_field, rdm_data_type from rdm_sync.field_mapping where code=? and version=?",
                (rs, rowNum) -> new FieldMapping(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                ), code, version);
    }


}
