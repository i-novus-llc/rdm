package ru.i_novus.ms.rdm.sync.service;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public interface RdmMappingService {
    /**
     * Преобразование объекта согласно маппингу.
     *
     * @param rdmType тип данных в НСИ
     * @param sysType тип данных в системе
     * @param value        значение для преобразования
     * @return преобразованное значение
     */
    Object map(FieldType rdmType, DataTypeEnum sysType, Object value);
}
