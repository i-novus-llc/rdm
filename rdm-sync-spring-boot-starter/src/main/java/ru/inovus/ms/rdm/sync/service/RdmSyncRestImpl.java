package ru.inovus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.inovus.ms.rdm.sync.RdmClientSyncConfig;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

public class RdmSyncRestImpl implements RdmSyncRest {
    public static final Logger logger = LoggerFactory.getLogger(RdmSyncRestImpl.class);

    @Autowired
    private RefBookService refBookService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private CompareService compareService;
    @Autowired
    private RdmMappingService rdmMappingService;
    @Autowired
    private RdmSyncDao dao;
    private RdmClientSyncConfig config;

    public RdmSyncRestImpl(RdmClientSyncConfig config) {
        this.config = config;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update() {
        Map<VersionMapping, RefBook> refbooksToUpdate = getRefbooksToUpdate();
        for (Map.Entry<VersionMapping, RefBook> entry : refbooksToUpdate.entrySet()) {
            VersionMapping versionMapping = entry.getKey();
            RefBook newVersion = entry.getValue();
            if (versionMapping.getVersion() == null) {
                //заливаем с нуля
                uploadNew(versionMapping);
            } else {
                //обновляем данные
                mergeData(versionMapping, newVersion);
            }
            //обновляем версию в таблице версий клиента
            updateVersion(versionMapping.getId(), newVersion);
            //обновляем версию в таблице маппинга полей
            dao.updateFieldMappingVersion(newVersion.getLastPublishedVersion());
        }
    }

    private void mergeData(VersionMapping versionMapping, RefBook newVersion) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setOldVersionId(versionService.getByVersionAndCode(versionMapping.getVersion(), versionMapping.getCode()).getId());
        compareDataCriteria.setNewVersionId(newVersion.getId());
        compareDataCriteria.setCountOnly(true);
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);
        //todo если изменилась структура и изменения коснулись полей в маппинге, то выбрасываем исключение
        List<String> clientFields = rdmMappingService.getFieldMapping(versionMapping.getCode(), versionMapping.getVersion()).stream().map(FieldMapping::getRdmField).collect(Collectors.toList());
//        if (!CollectionUtils.isEmpty(diff.getOldAttributes()) && diff.getOldAttributes().contains())
        if (diff.getRows().getTotalElements() > 0) {
            compareDataCriteria.setCountOnly(false);
            for (int i = 0; i < diff.getRows().getTotalPages(); i++) {
                compareDataCriteria.setPageNumber(i);
                diff = compareService.compareData(compareDataCriteria);
                for (DiffRowValue row : diff.getRows()) {
                    LinkedHashMap<String, Object> mappedRow = rdmMappingService.map(versionMapping.getCode(), versionMapping.getVersion(), row);
                    switch (row.getStatus()) {
                        case INSERTED:
                            dao.insertRow(versionMapping.getTable(), mappedRow);
                            break;
                        case DELETED:


                    }
                }
            }
        }
    }

    private void uploadNew(VersionMapping versionMapping) {
        String primaryField = versionMapping.getPrimaryField();
        List<Object> existingDataIds = dao.getDataIds(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField());
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        int page = 1;
        Page<RefBookRowValue> list = versionService.search(versionMapping.getCode(), searchDataCriteria);
        while (page <= list.getTotalPages()) {
            for (RefBookRowValue rdmRowValue : list) {
                LinkedHashMap<String, Object> mappedRow = rdmMappingService.map(versionMapping.getCode(), versionMapping.getVersion(), rdmRowValue);
                Object primaryValue = mappedRow.get(primaryField);
                if (existingDataIds.contains(primaryValue)) {
                    //update
                    dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), mappedRow);
                } else {
                    //insert
                    dao.insertRow(versionMapping.getTable(), mappedRow);
                }
            }
            if (page < list.getTotalPages()) {
                searchDataCriteria.setPageNumber(page);
                list = versionService.search(versionMapping.getCode(), searchDataCriteria);
            }
            page++;
        }
    }


    private void updateVersion(Integer id, RefBook newVersion) {
        dao.updateVersionMapping(id, newVersion.getLastPublishedVersion(), newVersion.getLastPublishedVersionFromDate());
    }

    private Map<VersionMapping, RefBook> getRefbooksToUpdate() {
        List<VersionMapping> currentRefbooks = rdmMappingService.getVersionMappings();
        Map<VersionMapping, RefBook> refbooksToUpdate = new HashMap<>();
        RefBookCriteria refBookCriteria = new RefBookCriteria();
        for (VersionMapping currentRefbook : currentRefbooks) {
            refBookCriteria.setCode(currentRefbook.getCode());
            //актуальная версия справочника
            Page<RefBook> rdmRefbooks = refBookService.search(refBookCriteria);
            if (rdmRefbooks.isEmpty()) {
                //todo писать в журнал
                logError(currentRefbook.getCode(), currentRefbook.getVersion(),
                        String.format("Справочник с кодом %s не найден в системе", currentRefbook.getCode()));
            }
            RefBook rdmRefbook = rdmRefbooks.iterator().next();
            //проверяем наличие первичного ключа
            if (rdmRefbook.getStructure().getPrimary().isEmpty()) {
                logError(currentRefbook.getCode(), currentRefbook.getVersion(),
                        String.format("Невозможно обновить справочник с кодом %s: отсутствует первичный ключ", currentRefbook.getCode()));
            }
            //сравниваем по версии и дате публикации
            if (!currentRefbook.getVersion().equals(rdmRefbook.getLastPublishedVersion()) &&
                    !currentRefbook.getPublicationDate().equals(rdmRefbook.getLastPublishedVersionFromDate())) {
                refbooksToUpdate.put(currentRefbook, rdmRefbook);
            }
        }
        return refbooksToUpdate;
    }

    private void logError(String code, String version, String message) {
        logger.error(message);
        //todo писать в журнал
    }
}
