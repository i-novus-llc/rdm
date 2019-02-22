package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public class RdmMappingServiceImpl implements RdmMappingService {

    @Autowired
    private RdmSyncDao dao;

    @Override
    public LinkedHashMap<String, Object> map(String refbookCode, String version, DiffRowValue diffRowValue) {
        List<FieldMapping> fieldMappings = getFieldMapping(refbookCode, version);
        LinkedHashMap<String, Object> mappedValues = new LinkedHashMap<>();
        for (DiffFieldValue rdmValue : diffRowValue.getValues()) {
            FieldMapping fieldMapping = fieldMappings.stream().filter(f -> f.getRdmField().equals(rdmValue.getField().getName())).findAny().orElse(null);
            if (fieldMapping == null) {
                //поле не ведется у клиента
                continue;
            }
            Field rdmField = rdmValue.getField();
            //если типы данных бд у полей совпадают, то сохраняем данные без изменений
            if (rdmField.getType().equals(fieldMapping.getSysDataType())) {
                mappedValues.put(fieldMapping.getSysField(), rdmValue.getNewValue());
                continue;
            }
        }
        return mappedValues;
    }

    @Override
    public LinkedHashMap<String, Object> map(String refbookCode, String version, RefBookRowValue rowValue) {
        return null;
    }

    @Override
    public List<VersionMapping> getVersionMappings() {
        return dao.getVersionMappings();
    }

    @Override
    public List<FieldMapping> getFieldMapping(String refbookCode, String version) {
        return dao.getFieldMapping(refbookCode, version);
    }

}
