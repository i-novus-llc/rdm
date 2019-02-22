package ru.inovus.ms.rdm.sync.service;

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
public interface RdmMappingService {
    LinkedHashMap<String, Object> map(String refbookCode, String version, DiffRowValue diffRowValue);

    LinkedHashMap<String, Object> map(String refbookCode, String version, RefBookRowValue rowValue);

    List<VersionMapping> getVersionMappings();

    List<FieldMapping> getFieldMapping(String code, String version);
}
