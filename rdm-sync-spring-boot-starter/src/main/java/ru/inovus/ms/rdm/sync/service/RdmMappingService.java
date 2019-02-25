package ru.inovus.ms.rdm.sync.service;

import ru.inovus.ms.rdm.sync.model.FieldMapping;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public interface RdmMappingService {
    /**
     * Преобразование объекта согласно маппингу.
     *
     * @param fieldMapping маппинг
     * @param value        значение для преобразования
     * @return преобразованное значение
     */
    Object map(FieldMapping fieldMapping, Object value);
}
