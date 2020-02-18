package ru.inovus.ms.rdm.sync.service;

import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.Log;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingRefBook;

import javax.ws.rs.core.MultivaluedMap;
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

    int getLastVersion(String refbookCode);

    /**
     * Получить список маппинга полей справочников НСИ на поля клиента
     *
     * @param refbookCode код справочника НСИ
     * @return список
     */
    List<FieldMapping> getFieldMapping(String refbookCode);

    List<Pair<String, String>> getColumnNameAndDataTypeFromLocalDataTable(String table);

    void updateVersionMapping(Integer id, String version, LocalDateTime publishDate);

    /**
     *
     * @param table          таблица справочника на стороне клиента
     * @param primaryField   поле, являющееся первичном ключом справочника, в таблице клиента
     * @return список идентификаторов данных справочника клиента
     */
    List<Object> getDataIds(String table, FieldMapping primaryField);

    /**
     *
     * @param table таблица справочника на стороне клиента
     * @param primaryField поле, являющееся первичном ключом справочника, в таблице клиента
     * @return true, если идентификатор есть в таблице
     */
    boolean isIdExists(String table, String primaryField, Object primaryValue);

    /**
     * Вставить строку в справочник клиента
     *
     * @param table таблица справочника на стороне клиента
     * @param row   строка с данными
     */
    void insertRow(String table, Map<String, Object> row, boolean markSynced);

    /**
     * Изменить строку в справочник клиента
     * @param table таблица справочника на стороне клиента
     * @param primaryField поле, являющееся первичном ключом справочника, в таблице клиента
     * @param row строка с данными
     */
    void updateRow(String table, String primaryField, Map<String, Object> row, boolean markSynced);

    /**
     * Пометить запись справочника клиента (не)удаленной
     * @param table таблица справочника на стороне клиента
     * @param primaryField поле, являющееся первичном ключом справочника, в таблице клиента
     * @param isDeletedField поле, отвечающее за признак удаления, в таблице клиента
     * @param primaryValue значение первичного ключа записи
     * @param deleted новое значение для поля isDeletedField
     */
    void markDeleted(String table, String primaryField, String isDeletedField, Object primaryValue, boolean deleted, boolean markSynced);

    /**
     * Пометить все записи справочника клиента (не)удаленными
     * @param table таблица справочника на стороне клиента
     * @param isDeletedField поле, отвечающее за признак удаления, в таблице клиента
     * @param deleted новое значение для поля isDeletedField
     */
    void markDeleted(String table, String isDeletedField, boolean deleted, boolean markSynced);

    void log(String status, String refbookCode, String oldVersion, String newVersion, String message, String stack);

    List<Log> getList(LocalDate date, String refbookCode);

    void upsertVersionMapping(XmlMappingRefBook versionMapping);

    void insertFieldMapping(String code, List<XmlMappingField> fieldMappings);

    boolean lockRefBookForUpdate(String code, boolean blocking);

    void addInternalLocalRowStateUpdateTrigger(String schema, String table);
    void createOrReplaceLocalRowStateUpdateFunction();
    void addInternalLocalRowStateColumnIfNotExists(String schema, String table);
    void disableInternalLocalRowStateUpdateTrigger(String table);
    void enableInternalLocalRowStateUpdateTrigger(String table);

    Page<Map<String, Object>> getData(String table, String pk, int limit, int offset, RdmSyncLocalRowState state, MultivaluedMap<String, Object> filters);
    <T> boolean setLocalRecordsState(String table, String pk, List<? extends T> primaryValues, RdmSyncLocalRowState expectedState, RdmSyncLocalRowState state);
    RdmSyncLocalRowState getLocalRowState(String table, String pk, Object pv);

    void createSchemaIfNotExists(String schema);
    void createRefBookTableIfNotExists(String schema, String table, List<FieldMapping> fieldMappings, String isDeletedFieldName);

}
