package ru.inovus.ms.rdm.sync.service;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.inovus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.inovus.ms.rdm.api.model.diff.StructureDiff;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.service.CompareService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.sync.criteria.LogCriteria;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.Log;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.DELETED;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.INSERTED;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

public class RdmSyncRestImpl implements RdmSyncRest {
    private static final Logger logger = LoggerFactory.getLogger(RdmSyncRestImpl.class);
    private static final int MAX_SIZE = 100;

    @Autowired
    private RefBookService refBookService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private CompareService compareService;
    @Autowired
    private RdmMappingService mappingService;
    @Autowired
    private RdmLoggingService loggingService;
    @Autowired
    private RdmSyncRest self;
    @Autowired
    private RdmSyncDao dao;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update() {
        List<VersionMapping> refbooks = dao.getVersionMappings();
        for (VersionMapping refbook : refbooks) {
            self.update(refbook.getCode());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(String refbookCode) {
        VersionMapping versionMapping = getVersionMapping(refbookCode);
        RefBook newVersion;
        try {
            newVersion = getNewVersionFromRdm(refbookCode);
        } catch (RuntimeException e) {
            logger.error(String.format("Ошибка при получении новой версии справочника с кодом %s", refbookCode), e);
            loggingService.logError(refbookCode, null, null, e.getMessage(), ExceptionUtils.getStackTrace(e));
            return;
        }
        try {
            if (versionMapping.getVersion() == null) {
                //заливаем с нуля
                uploadNew(versionMapping, newVersion);
            } else if (!versionMapping.getVersion().equals(newVersion.getLastPublishedVersion()) &&
                    !versionMapping.getPublicationDate().equals(newVersion.getLastPublishedVersionFromDate())) {
                //если версия и дата публикация не совпадают - нужно обновить справочник
                mergeData(versionMapping, newVersion);
            } else if (versionMapping.changed()) {
//              Значит в прошлый раз мы синхронизировались по старому маппингу.
//              Необходимо полностью залить свежую версию.
                dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true);
                uploadNew(versionMapping, newVersion);
            }
            //обновляем версию в таблице версий клиента
            dao.updateVersionMapping(versionMapping.getId(), newVersion.getLastPublishedVersion(), newVersion.getLastPublishedVersionFromDate());
        } catch (RuntimeException e) {
            logger.error(String.format("Ошибка при обновлении справочника с кодом %s", refbookCode), e);
            loggingService.logError(refbookCode, versionMapping.getVersion(), newVersion.getLastPublishedVersion(), e.getMessage(), ExceptionUtils.getStackTrace(e));
            return;
        }
        loggingService.logOk(refbookCode, versionMapping.getVersion(), newVersion.getLastPublishedVersion());
    }

    @Override
    public List<Log> getLog(LogCriteria criteria) {
        return loggingService.getList(criteria.getDate(), criteria.getRefbookCode());
    }

    private VersionMapping getVersionMapping(String refbookCode) {
        VersionMapping versionMapping = dao.getVersionMapping(refbookCode);
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        if (fieldMappings.stream().noneMatch(f -> f.getSysField().equals(versionMapping.getPrimaryField()))) {
            throw new IllegalArgumentException(String.format("Поле %s, указанное в качестве первичного ключа, не задано в маппинге полей", versionMapping.getPrimaryField()));
        }
        return versionMapping;
    }

    private RefBook getNewVersionFromRdm(String refbookCode) {
        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setCode(refbookCode);
        refBookCriteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);
        Page<RefBook> rdmRefbooks = refBookService.search(refBookCriteria);
        if (CollectionUtils.isEmpty(rdmRefbooks.getContent())) {
            throw new IllegalStateException(String.format("Справочник с кодом %s не найден в системе", refbookCode));
        }
        RefBook rdmRefbook = rdmRefbooks.getContent().get(0);
        //проверяем наличие первичного ключа
        if (rdmRefbook.getStructure().getPrimary().isEmpty()) {
            throw new IllegalStateException(String.format("Невозможно обновить справочник с кодом %s: отсутствует первичный ключ", refbookCode));
        }
        return rdmRefbook;
    }

    private void mergeData(VersionMapping versionMapping, RefBook newVersion) {
        Integer oldVersionId = versionService.getVersion(versionMapping.getVersion(), versionMapping.getCode()).getId();
        StructureDiff structureDiff = compareService.compareStructures(oldVersionId, newVersion.getId());
        if (!CollectionUtils.isEmpty(structureDiff.getUpdated()) || !CollectionUtils.isEmpty(structureDiff.getDeleted()) || !CollectionUtils.isEmpty(structureDiff.getInserted())) {
            dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true);
            uploadNew(versionMapping, newVersion);
            return;
        }
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setOldVersionId(oldVersionId);
        compareDataCriteria.setNewVersionId(newVersion.getId());
        compareDataCriteria.setCountOnly(true);
        compareDataCriteria.setPageSize(1);
        int page = 0;
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);
        //если изменилась структура, проверяем актуальность полей в маппинге
        validateStructureChanges(versionMapping, fieldMappings, diff);
        if (diff.getRows().getTotalElements() > 0) {
            compareDataCriteria.setCountOnly(false);
            compareDataCriteria.setPageSize(MAX_SIZE);
            for (int i = 0; i < diff.getRows().getTotalElements(); i = i + MAX_SIZE) {
                compareDataCriteria.setPageNumber(page);
                diff = compareService.compareData(compareDataCriteria);
                for (DiffRowValue row : diff.getRows().getContent()) {
                    mergeRow(row, versionMapping, fieldMappings, newVersion);
                }
                page++;
            }
        }
    }

    private void mergeRow(DiffRowValue row, VersionMapping versionMapping, List<FieldMapping> fieldMappings, RefBook newVersion) {
        Map<String, Object> mappedRow = new HashMap<>();
        for (DiffFieldValue diffFieldValue : row.getValues()) {
            Map<String, Object> mappedValue = mapValue(diffFieldValue.getField().getName(),
                    DELETED.equals(row.getStatus()) ? diffFieldValue.getOldValue() : diffFieldValue.getNewValue(),
                    fieldMappings, newVersion);
            if (mappedValue != null)
                mappedRow.putAll(mappedValue);
        }
        Object primaryValue = mappedRow.get(versionMapping.getPrimaryField());
        boolean idExists = dao.isIdExists(versionMapping.getTable(), versionMapping.getPrimaryField(), primaryValue);
        if (DELETED.equals(row.getStatus())) {
            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), primaryValue, true);
        } else if (INSERTED.equals(row.getStatus()) && !idExists) {
            dao.insertRow(versionMapping.getTable(), mappedRow);
        } else {
            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), primaryValue, false);
            dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), mappedRow);
        }
    }

    private Map<String, Object> mapValue(String rdmField, Object value, List<FieldMapping> fieldMappings, RefBook newVersion) {
        FieldMapping fieldMapping = fieldMappings.stream().filter(m -> m.getRdmField().equals(rdmField)).findAny().orElse(null);
        if (fieldMapping == null) {
            //поле не ведется в системе
            return null;
        }
        FieldType rdmType = newVersion.getStructure().getAttribute(fieldMapping.getRdmField()).getType();
        DataTypeEnum clientType = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());
        Map<String, Object> mappedValue = new HashMap<>();
        mappedValue.put(fieldMapping.getSysField(), mappingService.map(rdmType, clientType, value));
        return mappedValue;
    }

    private void validateStructureChanges(VersionMapping versionMapping, List<FieldMapping> fieldMappings, RefBookDataDiff diff) {
        List<String> clientRdmFields = fieldMappings.stream().map(FieldMapping::getRdmField).collect(Collectors.toList());
        //проверяем удаленные поля
        if (!CollectionUtils.isEmpty(diff.getOldAttributes())) {
            diff.getOldAttributes().retainAll(clientRdmFields);
            if (!diff.getOldAttributes().isEmpty()) {
                //в новой версии удалены поля, которые ведутся в системе
                throw new IllegalStateException(String.format("В новой версии справочника с кодом %s удалены поля %s. Обновите маппинг",
                        versionMapping.getCode(), String.join(",", diff.getOldAttributes())));
            }
        }
    }

    public void uploadNew(VersionMapping versionMapping, RefBook newVersion) {
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        List<Object> existingDataIds = dao.getDataIds(versionMapping.getTable(),
                fieldMappings.stream().filter(f -> f.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElse(null));
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setPageSize(1);
        int page = 0;
        Page<RefBookRowValue> list = versionService.search(versionMapping.getCode(), searchDataCriteria);
        searchDataCriteria.setPageSize(MAX_SIZE);
        for (int i = 0; i < list.getTotalElements(); i = i + MAX_SIZE) {
            searchDataCriteria.setPageNumber(page);
            list = versionService.search(versionMapping.getCode(), searchDataCriteria);
            for (RefBookRowValue row : list.getContent()) {
                insertOrUpdateRow(row, existingDataIds, versionMapping, fieldMappings, newVersion);
            }
            page++;
        }
    }

    private void insertOrUpdateRow(RefBookRowValue row, List<Object> existingDataIds, VersionMapping versionMapping, List<FieldMapping> fieldMappings, RefBook newVersion) {
        String primaryField = versionMapping.getPrimaryField();
        Map<String, Object> mappedRow = new HashMap<>();
        for (FieldValue fieldValue : row.getFieldValues()) {
            Map<String, Object> mappedValue = mapValue(fieldValue.getField(), fieldValue.getValue(), fieldMappings, newVersion);
            if (mappedValue != null)
                mappedRow.putAll(mappedValue);
        }
        Object primaryValue = mappedRow.get(primaryField);
        if (existingDataIds.contains(primaryValue)) {
            //если запись существует, обновляем
            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), primaryValue, false);
            dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), mappedRow);
        } else {
            //создаем новую запись
            dao.insertRow(versionMapping.getTable(), mappedRow);
        }
    }
}
