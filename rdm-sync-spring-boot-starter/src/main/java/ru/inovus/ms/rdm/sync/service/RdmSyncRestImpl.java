package ru.inovus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

public class RdmSyncRestImpl implements RdmSyncRest {
    public static final Logger logger = LoggerFactory.getLogger(RdmSyncRestImpl.class);
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
    private RdmSyncRest self;
    @Autowired
    private RdmSyncDao dao;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update() {
        List<VersionMapping> refbooks = dao.getVersionMappings();
        for (VersionMapping refbook : refbooks) {
            try {
                self.update(refbook.getCode());
            } catch (RuntimeException e) {
                logger.error(String.format("Ошибка при обновлении справочника с кодом %s", refbook.getCode()), e);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(String refbookCode) {
        VersionMapping versionMapping = getVersionMapping(refbookCode);
        RefBook newVersion = getNewVersionFromRdm(refbookCode);
        if (versionMapping.getVersion() == null) {
            //заливаем с нуля
            uploadNew(versionMapping);
        } else if (!versionMapping.getVersion().equals(newVersion.getLastPublishedVersion()) &&
                !versionMapping.getPublicationDate().equals(newVersion.getLastPublishedVersionFromDate())) {
            //если версия и дата публикация не совпадают - нужно обновить справочник
            mergeData(versionMapping, newVersion);
        }
        //обновляем версию в таблице версий клиента
        dao.updateVersionMapping(versionMapping.getId(), newVersion.getLastPublishedVersion(), newVersion.getLastPublishedVersionFromDate());
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
        refBookCriteria.setHasPublished(true);
        Page<RefBook> rdmRefbooks = refBookService.search(refBookCriteria);
        if (rdmRefbooks.getContent() == null || rdmRefbooks.getContent().isEmpty()) {
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
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setOldVersionId(versionService.getByVersionAndCode(versionMapping.getVersion(), versionMapping.getCode()).getId());
        compareDataCriteria.setNewVersionId(newVersion.getId());
        compareDataCriteria.setCountOnly(true);
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);
        //если изменилась структура, проверяем актуальность полей в маппинге
        validateStructureChanges(versionMapping, fieldMappings, diff, newVersion);
        if (diff.getRows().getTotalElements() > 0) {
            compareDataCriteria.setCountOnly(false);
            for (int i = 0; i < diff.getRows().getTotalElements(); i = i + MAX_SIZE) {
                compareDataCriteria.setPageNumber(i);
                diff = compareService.compareData(compareDataCriteria);
                for (DiffRowValue row : diff.getRows().getContent()) {
                    LinkedHashMap<String, Object> mappedRow = new LinkedHashMap<>();
                    for (DiffFieldValue diffFieldValue : row.getValues()) {
                        FieldMapping fieldMapping = fieldMappings.stream().filter(m -> m.getRdmField().equals(diffFieldValue.getField().getName())).findAny().orElse(null);
                        if (fieldMapping == null) {
                            //поле не ведется в системе
                            continue;
                        }
                        Object mappedValue = mappingService.map(fieldMapping,
                                DiffStatusEnum.DELETED.equals(row.getStatus()) ? diffFieldValue.getOldValue() : diffFieldValue.getNewValue());
                        mappedRow.put(fieldMapping.getSysField(), mappedValue);
                    }
                    switch (row.getStatus()) {
                        case INSERTED:
                            dao.insertRow(versionMapping.getTable(), mappedRow);
                            break;
                        case DELETED:
                            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(),
                                    mappedRow.get(versionMapping.getPrimaryField()));
                            break;
                        case UPDATED:
                            dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), mappedRow);
                    }
                }
            }
        }
    }

    private void validateStructureChanges(VersionMapping versionMapping, List<FieldMapping> fieldMappings,
                                          RefBookDataDiff diff, RefBook newVersion) {
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
        if (!CollectionUtils.isEmpty(diff.getUpdatedAttributes())) {
            for (String updatedAttribute : diff.getUpdatedAttributes()) {
                FieldMapping fieldMapping = fieldMappings.stream().filter(f -> f.getRdmField().equals(updatedAttribute)).findAny().orElse(null);
                if (fieldMapping == null) {
                    continue;
                }
                DataTypeEnum rdmType = DataTypeEnum.getByNsiDataType(newVersion.getStructure().getAttribute(updatedAttribute).getType().name());
                DataTypeEnum clientType = DataTypeEnum.getByText(fieldMapping.getSysDataType());
                //проверяем изменения в типе данных
                if (!rdmType.equals(clientType)) {
                    throw new IllegalStateException(String.format("В новой версии справочника с кодом %s изменен тип поля %s с %s на %s. Обновите маппинг",
                            versionMapping.getCode(), updatedAttribute, fieldMapping.getRdmDataType(),
                            DataTypeEnum.valueOf(rdmType.name())));
                }
            }
        }
    }

    private void uploadNew(VersionMapping versionMapping) {
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        String primaryField = versionMapping.getPrimaryField();
        List<Object> existingDataIds = dao.getDataIds(versionMapping.getTable(),
                fieldMappings.stream().filter(f -> f.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElse(null), versionMapping.getDeletedField());
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setPageSize(1);
        int page = 0;
        Page<RefBookRowValue> list = versionService.search(versionMapping.getCode(), searchDataCriteria);
        searchDataCriteria.setPageSize(MAX_SIZE);
        for (int i = 0; i < list.getTotalElements(); i = i + MAX_SIZE) {
            searchDataCriteria.setPageNumber(page);
            list = versionService.search(versionMapping.getCode(), searchDataCriteria);
            for (RefBookRowValue rdmRowValue : list.getContent()) {
                LinkedHashMap<String, Object> mappedRow = new LinkedHashMap<>();
                for (FieldValue fieldValue : rdmRowValue.getFieldValues()) {
                    FieldMapping fieldMapping = fieldMappings.stream().filter(m -> m.getRdmField().equals(fieldValue.getField())).findAny().orElse(null);
                    if (fieldMapping == null) {
                        //поле не ведется в системе
                        continue;
                    }
                    Object mappedValue = mappingService.map(fieldMapping, fieldValue.getValue());
                    mappedRow.put(fieldMapping.getSysField(), mappedValue);
                }
                Object primaryValue = mappedRow.get(primaryField);
                if (existingDataIds.contains(primaryValue)) {
                    //если запись существует, обновляем
                    dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), mappedRow);
                } else {
                    //создаем новую запись
                    dao.insertRow(versionMapping.getTable(), mappedRow);
                }
            }
            page++;
        }
    }
}
