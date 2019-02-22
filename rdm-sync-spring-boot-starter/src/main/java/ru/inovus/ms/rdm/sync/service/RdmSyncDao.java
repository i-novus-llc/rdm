package ru.inovus.ms.rdm.sync.service;

import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public interface RdmSyncDao {

    /**
     * Получить поле клиента, отвечающее за признак удаления
     * @param refbookCode код справочника НСИ
     * @param version номер версии справочника в НСИ
     * @return поле клиента, отвечающее за признак удаления
     */
    String getDeletedField(String refbookCode, String version);

    /**
     * Получить список маппинга справочников НСИ на таблицы клиента
     * @return список
     */
    List<VersionMapping> getVersionMappings();

    /**
     * Получить список маппинга полей справочников НСИ на поля клиента
     * @param refbookCode код справочника НСИ
     * @param version номер версии справочника в НСИ
     * @return список
     */
    List<FieldMapping> getFieldMapping(String refbookCode, String version);

    void updateVersionMapping(Integer id, String version, LocalDateTime publishDate);

    void updateFieldMappingVersion(String version);

    /**
     *
     * @param table таблица справочника на стороне клиента
     * @param primaryField поле, являющееся первичном ключом справочника, в таблице клиента
     * @param isDeletedField поле, отвечающее за признак удаления, в таблице клиента
     * @return список идентификаторов данных справочника клиента
     */
    List<Object> getDataIds(String table, String primaryField, String isDeletedField);

    /**
     * Вставить строку в справочник клиента
     * @param table таблица справочника на стороне клиента
     * @param row строка с данными
     */
    void insertRow(String table, LinkedHashMap<String, Object> row);

    void updateRow(String table, String primaryField, String isDeletedField, LinkedHashMap<String, Object> row);

    void markDeleted(String table, String primaryField, String isDeletedField, Object primaryValue);
}
