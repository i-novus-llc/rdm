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
     * Получить список маппинга справочников НСИ на таблицы клиента
     * @return список
     */
    List<VersionMapping> getVersionMappings();

    VersionMapping getVersionMapping(String refbookCode);

    /**
     * Получить список маппинга полей справочников НСИ на поля клиента
     * @param refbookCode код справочника НСИ
     * @return список
     */
    List<FieldMapping> getFieldMapping(String refbookCode);

    void updateVersionMapping(Integer id, String version, LocalDateTime publishDate);

    /**
     *
     * @param table таблица справочника на стороне клиента
     * @param primaryField поле, являющееся первичном ключом справочника, в таблице клиента
     * @param isDeletedField поле, отвечающее за признак удаления, в таблице клиента
     * @return список идентификаторов данных справочника клиента
     */
    List<Object> getDataIds(String table, FieldMapping primaryField, String isDeletedField);

    /**
     * Вставить строку в справочник клиента
     * @param table таблица справочника на стороне клиента
     * @param row строка с данными
     */
    void insertRow(String table, LinkedHashMap<String, Object> row);

    void updateRow(String table, String primaryField, String isDeletedField, LinkedHashMap<String, Object> row);

    void markDeleted(String table, String primaryField, String isDeletedField, Object primaryValue);
}
