package ru.inovus.ms.rdm.sync.service;

import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.Log;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public interface RdmSyncDao {

    /**
     * Получить список маппинга справочников НСИ на таблицы клиента
     *
     * @return список
     */
    List<VersionMapping> getVersionMappings();

    VersionMapping getVersionMapping(String refbookCode);

    /**
     * Получить список маппинга полей справочников НСИ на поля клиента
     *
     * @param refbookCode код справочника НСИ
     * @return список
     */
    List<FieldMapping> getFieldMapping(String refbookCode);

    void updateVersionMapping(Integer id, String version, LocalDateTime publishDate);

    /**
     * @param table          таблица справочника на стороне клиента
     * @param primaryField   поле, являющееся первичном ключом справочника, в таблице клиента
     * @param isDeletedField поле, отвечающее за признак удаления, в таблице клиента
     * @return список идентификаторов данных справочника клиента
     */
    List<Object> getDataIds(String table, FieldMapping primaryField, String isDeletedField);

    /**
     * Вставить строку в справочник клиента
     *
     * @param table таблица справочника на стороне клиента
     * @param row   строка с данными
     */
    void insertRow(String table, Map<String, Object> row);

    /**
     * Изменить строку в справочник клиента
     * @param table таблица справочника на стороне клиента
     * @param primaryField поле, являющееся первичном ключом справочника, в таблице клиента
     * @param isDeletedField поле, отвечающее за признак удаления, в таблице клиента
     * @param row строка с данными
     */
    void updateRow(String table, String primaryField, String isDeletedField, Map<String, Object> row);

    /**
     * Пометить запись справочника клиента удаленной
     * @param table таблица справочника на стороне клиента
     * @param primaryField поле, являющееся первичном ключом справочника, в таблице клиента
     * @param isDeletedField поле, отвечающее за признак удаления, в таблице клиента
     * @param primaryValue значение первичного ключа записи
     */
    void markDeleted(String table, String primaryField, String isDeletedField, Object primaryValue);

    void log(String status, String refbookCode, String oldVersion, String newVersion, String message, String stack);

    List<Log> getList(LocalDate date, String refbookCode);
}
