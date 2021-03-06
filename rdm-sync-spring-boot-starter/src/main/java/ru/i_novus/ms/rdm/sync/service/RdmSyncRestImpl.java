package ru.i_novus.ms.rdm.sync.service;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.sync.criteria.LogCriteria;
import ru.i_novus.ms.rdm.sync.model.*;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.rest.RdmSyncRest;
import ru.i_novus.ms.rdm.sync.util.RefBookReferenceSort;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.DELETED;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.INSERTED;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@SuppressWarnings({"java:S3740","I-novus:MethodNameWordCountRule"})
public class RdmSyncRestImpl implements RdmSyncRest {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncRestImpl.class);
    private static final int MAX_SIZE = 100;

    private static final String ERROR_WHILE_FETCHING_NEW_VERSION    = "Error while fetching new version with code %s.";
    private static final String ERROR_WHILE_UPDATING_NEW_VERSION    = "Error while updating new version with code %s.";
    private static final String NO_MAPPING_FOR_PRIMARY_KEY          = "No mapping found for primary key %s.";
    private static final String NO_REFBOOK_FOUND                    = "No reference book with code %s found.";
    private static final String NO_PRIMARY_KEY_FOUND                = "No primary key found in reference book with code %s.";
    private static final String MAPPING_OUT_OF_DATE                 = "Field %s was deleted in version %s. Update your mappings.";
    private static final String COMPOSITE_PK_NOT_SUPPORTED          = "RefBook %s has composite primary key. They are not implemented yet.";
    private static final String MORE_THAN_ONE_REFBOOK_FOUND         = "Search for RefBook with code %s returned more than one element.";

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
    private RdmSyncDao dao;

    private RdmSyncRest self;

    @Autowired
    public void setSelf(RdmSyncRest self) {
        this.self = self;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update() {
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        List<RefBook> refBooks = getRefBooks(versionMappings);
        for (String code : RefBookReferenceSort.getSortedCodes(refBooks)) {
            self.update(
                refBooks.stream().filter(refBook -> refBook.getCode().equals(code)).findFirst().orElseThrow(),
                versionMappings.stream().filter(versionMapping -> versionMapping.getCode().equals(code)).findFirst().orElseThrow()
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update(String refBookCode) {
        if (dao.getVersionMapping(refBookCode) != null) {
            RefBook newVersion;
            try {
                newVersion = getLastPublishedVersionFromRdm(refBookCode);
            } catch (Exception e) {
                logger.error(format(ERROR_WHILE_FETCHING_NEW_VERSION, refBookCode), e);
                return;
            }
            VersionMapping versionMapping = getVersionMapping(refBookCode);
            try {
                if (isFirstLoad(versionMapping) || isNewVersionPublished(newVersion, versionMapping) || isMappingChanged(versionMapping)) {
                    self.update(newVersion, versionMapping);
                    loggingService.logOk(refBookCode, versionMapping.getVersion(), newVersion.getLastPublishedVersion());
                } else {
                    logger.info("Skipping update on {}. No changes.", refBookCode);
                }
            } catch (Exception e) {
                logger.error(format(ERROR_WHILE_UPDATING_NEW_VERSION, refBookCode), e);
                loggingService.logError(refBookCode, versionMapping.getVersion(), newVersion.getLastPublishedVersion(), e.getMessage(), ExceptionUtils.getStackTrace(e));
            }
        }
    }

    @Override
    @Transactional
    public void update(RefBook newVersion, VersionMapping versionMapping) {
        dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        try {
            if (isFirstLoad(versionMapping)) {
                //заливаем с нуля
                uploadNew(versionMapping, newVersion);
            } else if (isNewVersionPublished(newVersion, versionMapping)) {
                //если версия и дата публикация не совпадают - нужно обновить справочник
                mergeData(versionMapping, newVersion);
            } else if (isMappingChanged(versionMapping)) {
//              Значит в прошлый раз мы синхронизировались по старому маппингу.
//              Необходимо полностью залить свежую версию.
                dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
                uploadNew(versionMapping, newVersion);
            }
            //обновляем версию в таблице версий клиента
            dao.updateVersionMapping(versionMapping.getId(), newVersion.getLastPublishedVersion(), newVersion.getLastPublishedVersionFromDate());
        } finally {
            dao.enableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        }
    }

    @Override
    public List<Log> getLog(LogCriteria criteria) {
        return loggingService.getList(criteria.getDate(), criteria.getRefbookCode());
    }

    private boolean isFirstLoad(VersionMapping versionMapping) {
        return versionMapping.getVersion() == null;
    }

    private boolean isNewVersionPublished(RefBook newVersion, VersionMapping versionMapping) {
        return !versionMapping.getVersion().equals(newVersion.getLastPublishedVersion())
                && !versionMapping.getPublicationDate().equals(newVersion.getLastPublishedVersionFromDate());
    }

    private boolean isMappingChanged(VersionMapping versionMapping) {
        return versionMapping.changed();
    }

    @Override
    @Transactional(readOnly = true)
    public Response downloadXmlFieldMapping(List<String> forRefBooks) {
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        if (forRefBooks.stream().noneMatch("all"::equalsIgnoreCase))
            versionMappings = versionMappings.stream().filter(vm -> forRefBooks.contains(vm.getCode())).collect(toList());
        XmlMapping xmlMapping = new XmlMapping();
        xmlMapping.setRefbooks(new ArrayList<>());
        for (VersionMapping vm : versionMappings) {
            XmlMappingRefBook xmlMappingRefBook = XmlMappingRefBook.createBy(vm);
            xmlMappingRefBook.setField(dao.getFieldMapping(vm.getCode()).stream().map(XmlMappingField::createBy).collect(toList()));
            xmlMapping.getRefbooks().add(xmlMappingRefBook);
        }
        StreamingOutput stream = out -> {
            try {
                Marshaller marshaller = XmlMapping.JAXB_CONTEXT.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(xmlMapping, out);
                out.flush();
            } catch (JAXBException e) {
//              Не выбросится
                throw new RdmException(e);
            }
        };
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "filename=\"rdm-mapping.xml\"") .entity(stream).build();
    }

    private VersionMapping getVersionMapping(String refbookCode) {
        VersionMapping versionMapping = dao.getVersionMapping(refbookCode);
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        if (fieldMappings.stream().noneMatch(f -> f.getSysField().equals(versionMapping.getPrimaryField()))) {
            throw new IllegalArgumentException(format(NO_MAPPING_FOR_PRIMARY_KEY, versionMapping.getPrimaryField()));
        }
        return versionMapping;
    }

    public RefBook getLastPublishedVersionFromRdm(String code) {
        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);
        refBookCriteria.setCodeExact(code);
        Page<RefBook> page = refBookService.search(refBookCriteria);
        if (page.getContent().isEmpty())
            throw new IllegalArgumentException(format(NO_REFBOOK_FOUND, code));
        if (page.getContent().size() > 1)
            throw new IllegalStateException(format(MORE_THAN_ONE_REFBOOK_FOUND, code));
        RefBook last = page.getContent().iterator().next();
        if (last.getStructure().getPrimaries().isEmpty())
            throw new IllegalStateException(format(NO_PRIMARY_KEY_FOUND, code));
        if (last.getStructure().getPrimaries().size() > 1)
            throw new UnsupportedOperationException(String.format(COMPOSITE_PK_NOT_SUPPORTED, code));
        return last;
    }

    private List<RefBook> getRefBooks(List<VersionMapping> versionMappings) {
        List<RefBook> refBooks = new ArrayList<>();
        for (VersionMapping versionMapping : versionMappings) {
            try {
                refBooks.add(getLastPublishedVersionFromRdm(versionMapping.getCode()));
            } catch (RuntimeException ex) {
                logger.error(format(ERROR_WHILE_FETCHING_NEW_VERSION, versionMapping.getCode()), ex);
                loggingService.logError(versionMapping.getCode(), null, null, ex.getMessage(), ExceptionUtils.getStackTrace(ex));
            }
        }
        return refBooks;
    }

    private void mergeData(VersionMapping versionMapping, RefBook newVersion) {
        Integer oldVersionId = versionService.getVersion(versionMapping.getVersion(), versionMapping.getCode()).getId();
        StructureDiff structureDiff = compareService.compareStructures(oldVersionId, newVersion.getId());
        if (!CollectionUtils.isEmpty(structureDiff.getUpdated()) || !CollectionUtils.isEmpty(structureDiff.getDeleted()) || !CollectionUtils.isEmpty(structureDiff.getInserted())) {
            dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
            uploadNew(versionMapping, newVersion);
            return;
        }
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setOldVersionId(oldVersionId);
        compareDataCriteria.setNewVersionId(newVersion.getId());
        compareDataCriteria.setCountOnly(true);
        compareDataCriteria.setPageSize(1);
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);
        //если изменилась структура, проверяем актуальность полей в маппинге
        validateStructureChanges(versionMapping, fieldMappings, diff);
        if (diff.getRows().getTotalElements() > 0) {
            compareDataCriteria.setCountOnly(false);
            compareDataCriteria.setPageSize(MAX_SIZE);
            PageIterator<DiffRowValue, CompareDataCriteria> iter = new PageIterator<>(criteria -> compareService.compareData(criteria).getRows(), compareDataCriteria, true);
            while (iter.hasNext()) {
                Page<? extends DiffRowValue> page = iter.next();
                for (DiffRowValue diffRowValue : page.getContent()) {
                    mergeRow(diffRowValue, versionMapping, fieldMappings, newVersion);
                }
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
            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), primaryValue, true, true);
        } else if (INSERTED.equals(row.getStatus()) && !idExists) {
            dao.insertRow(versionMapping.getTable(), mappedRow, true);
        } else {
            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), primaryValue, false, true);
            dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), mappedRow, true);
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

        List<String> clientRdmFields = fieldMappings.stream().map(FieldMapping::getRdmField).collect(toList());

        // Проверка удалённых полей
        List<String> oldAttributes = diff.getAttributeDiff().getOldAttributes();
        if (!CollectionUtils.isEmpty(oldAttributes)) {
            oldAttributes.retainAll(clientRdmFields);
            if (!oldAttributes.isEmpty()) {
                //в новой версии удалены поля, которые ведутся в системе
                throw new IllegalStateException(format(MAPPING_OUT_OF_DATE,
                        String.join(",", oldAttributes), versionMapping.getCode()));
            }
        }
    }

    private void uploadNew(VersionMapping versionMapping, RefBook newVersion) {
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());
        List<Object> existingDataIds = dao.getDataIds(versionMapping.getTable(),
                fieldMappings.stream().filter(f -> f.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElse(null));
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setPageSize(MAX_SIZE);
        PageIterator<RefBookRowValue, SearchDataCriteria> iter = new PageIterator<>(criteria -> versionService.search(versionMapping.getCode(), criteria), searchDataCriteria, true);
        while (iter.hasNext()) {
            Page<? extends RefBookRowValue> page = iter.next();
            for (RefBookRowValue refBookRowValue : page.getContent()) {
                insertOrUpdateRow(refBookRowValue, existingDataIds, versionMapping, fieldMappings, newVersion);
            }
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
            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), primaryValue, false, true);
            dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), mappedRow, true);
        } else {
            //создаем новую запись
            dao.insertRow(versionMapping.getTable(), mappedRow, true);
        }
    }

}
