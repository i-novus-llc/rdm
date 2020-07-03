package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.components.common.exception.CodifiedException;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.exception.*;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refdata.*;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.inovus.ms.rdm.api.model.version.*;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.VersionFileService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.util.*;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.*;
import ru.inovus.ms.rdm.impl.file.FileStorage;
import ru.inovus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.inovus.ms.rdm.impl.file.process.*;
import ru.inovus.ms.rdm.impl.repository.*;
import ru.inovus.ms.rdm.impl.util.*;
import ru.inovus.ms.rdm.impl.util.mappers.*;
import ru.inovus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.inovus.ms.rdm.impl.validation.TypeValidation;
import ru.inovus.ms.rdm.impl.validation.VersionValidationImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity.objectPassportToValues;

@Primary
@Service
public class DraftServiceImpl implements DraftService {

    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";
    public static final String FILE_CONTENT_INVALID_EXCEPTION_CODE = "file.content.invalid";
    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private RefBookVersionRepository versionRepository;
    private RefBookConflictRepository conflictRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;
    private SearchDataService searchDataService;

    private RefBookLockService refBookLockService;
    private VersionService versionService;

    private FileStorage fileStorage;
    private FileNameGenerator fileNameGenerator;
    private VersionFileService versionFileService;

    private VersionValidation versionValidation;

    private PassportValueRepository passportValueRepository;
    private AttributeValidationRepository attributeValidationRepository;
    private StructureChangeValidator structureChangeValidator;

    private AuditLogService auditLogService;

    private int errorCountLimit = 100;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public DraftServiceImpl(RefBookVersionRepository versionRepository, RefBookConflictRepository conflictRepository,
                            DraftDataService draftDataService, DropDataService dropDataService, SearchDataService searchDataService,
                            RefBookLockService refBookLockService, VersionService versionService,
                            FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                            VersionFileService versionFileService,
                            VersionValidation versionValidation,
                            PassportValueRepository passportValueRepository,
                            AttributeValidationRepository attributeValidationRepository, StructureChangeValidator structureChangeValidator,
                            AuditLogService auditLogService) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
        this.searchDataService = searchDataService;

        this.refBookLockService = refBookLockService;
        this.versionService = versionService;

        this.fileStorage = fileStorage;
        this.fileNameGenerator = fileNameGenerator;
        this.versionFileService = versionFileService;

        this.versionValidation = versionValidation;

        this.passportValueRepository = passportValueRepository;
        this.attributeValidationRepository = attributeValidationRepository;
        this.structureChangeValidator = structureChangeValidator;

        this.auditLogService = auditLogService;
    }

    @Value("${rdm.validation-errors-count}")
    @SuppressWarnings("unused")
    public void setErrorCountLimit(int errorCountLimit) {
        this.errorCountLimit = errorCountLimit;
    }

    @Override
    @Transactional(timeout = 1200000)
    public Draft create(Integer refBookId, FileModel fileModel) {

        versionValidation.validateRefBook(refBookId);

        Draft draft;
        refBookLockService.setRefBookUpdating(refBookId);
        try {
            draft = updateDraftDataByFile(refBookId, fileModel);

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        auditLogService.addAction(AuditAction.UPLOAD_VERSION_FROM_FILE, () -> versionRepository.getOne(draft.getId()));

        return draft;
    }

    /** Обновление данных черновика справочника из файла. */
    private Draft updateDraftDataByFile(Integer refBookId, FileModel fileModel) {

        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());

        switch (FileUtil.getExtension(fileModel.getName())) {
            case "XLSX": return updateDraftDataByXlsx(refBookId, fileModel, inputStreamSupplier);
            case "XML": return updateDraftDataByXml(refBookId, fileModel, inputStreamSupplier);
            default: throw new FileExtensionException();
        }
    }

    private Draft updateDraftDataByXlsx(Integer refBookId, FileModel fileModel,
                                        Supplier<InputStream> inputStreamSupplier) {

        String extension = FileUtil.getExtension(fileModel.getName());
        BiConsumer<String, Structure> saveDraftConsumer = getSaveDraftConsumer(refBookId);
        RowsProcessor rowsProcessor = new CreateDraftBufferedRowsPersister(draftDataService, saveDraftConsumer);
        processFileRows(extension, rowsProcessor, new PlainRowMapper(), inputStreamSupplier);

        return getDraftByRefBook(refBookId).toDraft();
    }

    private Draft updateDraftDataByXml(Integer refBookId, FileModel fileModel,
                                       Supplier<InputStream> inputStreamSupplier) {

        try (XmlUpdateDraftFileProcessor xmlUpdateDraftFileProcessor = new XmlUpdateDraftFileProcessor(refBookId, this)) {
            Draft draft = xmlUpdateDraftFileProcessor.process(inputStreamSupplier);
            updateDraftData(versionRepository.getOne(draft.getId()), fileModel);
            return draft;
        }
    }

    /** Обновление данных черновика из файла. */
    private void updateDraftData(RefBookVersionEntity draft, FileModel fileModel) {

        Structure structure = draft.getStructure();

        String extension = FileUtil.getExtension(fileModel.getName());
        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());

        RowsProcessor rowsValidator = new RowsValidatorImpl(versionService, searchDataService,
                structure, draft.getStorageCode(), errorCountLimit, false,
                attributeValidationRepository.findAllByVersionId(draft.getId()));
        StructureRowMapper nonStrictOnTypeRowMapper = new NonStrictOnTypeRowMapper(structure, versionRepository);
        processFileRows(extension, rowsValidator, nonStrictOnTypeRowMapper, inputStreamSupplier);

        RowsProcessor rowsPersister = new BufferedRowsPersister(draftDataService, draft.getStorageCode(), structure);
        StructureRowMapper structureRowMapper = new StructureRowMapper(structure, versionRepository);
        processFileRows(extension, rowsPersister, structureRowMapper, inputStreamSupplier);
    }

    /** Обработка строк файла в соответствии с заданными параметрами. */
    private void processFileRows(String extension, RowsProcessor rowsProcessor,
                                 RowMapper rowMapper, Supplier<InputStream> fileSupplier) {

        try (FilePerRowProcessor persister = FileProcessorFactory.createProcessor(extension, rowsProcessor, rowMapper)) {
            persister.process(fileSupplier);

        } catch (NoSuchElementException e) {
            if (FILE_CONTENT_INVALID_EXCEPTION_CODE.equals(e.getMessage()))
                throw new FileContentException(e);

            throw e;

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    /**
     * Создание черновика при загрузке из XLSX (в виде consumer'а).
     * <p>
     * Таблица черновика не меняется,
     * т.к. уже изменена внутри CreateDraftBufferedRowsPersister.append .
     *
     * @param refBookId идентификатор справочника
     * @return consumer
     */
    private BiConsumer<String, Structure> getSaveDraftConsumer(Integer refBookId) {
        return (storageCode, structure) -> {

            RefBookVersionEntity lastRefBookVersion = getLastRefBookVersion(refBookId);
            RefBookVersionEntity draftVersion = getDraftByRefBook(refBookId);
            if (draftVersion == null && lastRefBookVersion == null)
                throw new NotFoundException(new Message(VersionValidationImpl.REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

            final String refBookCode = (draftVersion != null ? draftVersion : lastRefBookVersion).getRefBook().getCode();
            versionValidation.validateDraftStructure(refBookCode, structure);
            // Валидация структуры ссылочного справочника не нужна, т.к. все атрибуты строковые.

            if (draftVersion == null) {
                draftVersion = newDraftVersion(structure, lastRefBookVersion.getPassportValues());

            } else if (draftVersion.hasEmptyStructure()) {
                draftVersion.setStructure(structure);

            } else {
                removeDraft(draftVersion);
                versionRepository.flush(); // Delete old draft before insert new draft!

                draftVersion = newDraftVersion(structure, draftVersion.getPassportValues());
            }

            draftVersion.setRefBook(newRefBook(refBookId));
            draftVersion.setStorageCode(storageCode);

            versionRepository.save(draftVersion);
        };
    }

    @Override
    @Transactional
    public Draft create(CreateDraftRequest createDraftRequest) {

        final Integer refBookId = createDraftRequest.getRefBookId();
        versionValidation.validateRefBook(refBookId);

        RefBookVersionEntity lastRefBookVersion = getLastRefBookVersion(refBookId);
        RefBookVersionEntity draftVersion = getDraftByRefBook(refBookId);
        if (draftVersion == null && lastRefBookVersion == null)
            throw new NotFoundException(new Message(VersionValidationImpl.REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

        List<PassportValueEntity> passportValues = null;
        if (createDraftRequest.getPassport() != null) {
            passportValues = objectPassportToValues(createDraftRequest.getPassport(), true, null);
        }

        final Structure structure = createDraftRequest.getStructure();
        final String refBookCode = (draftVersion != null ? draftVersion : lastRefBookVersion).getRefBook().getCode();

        versionValidation.validateDraftStructure(refBookCode, structure);
        if (createDraftRequest.getReferrerValidationRequired())
            versionValidation.validateReferrerStructure(structure);

        List<Field> fields = ConverterUtil.fields(structure);
        if (draftVersion == null) {
            draftVersion = newDraftVersion(structure, passportValues != null ? passportValues : lastRefBookVersion.getPassportValues());
            draftVersion.setRefBook(newRefBook(refBookId));

            String draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);
            draftVersion.getRefBook().setCode(lastRefBookVersion.getRefBook().getCode());

        } else {
            draftVersion = recreateDraft(structure, draftVersion, fields, passportValues);
        }

        RefBookVersionEntity savedDraftVersion = versionRepository.save(draftVersion);
        addValidations(createDraftRequest.getValidations(), savedDraftVersion);

        return savedDraftVersion.toDraft();
    }

    private void addValidations(Map<String, List<AttributeValidation>> validations, RefBookVersionEntity entity) {
        if (validations != null) validations.forEach((attrCode, list) -> list.forEach(validation -> addAttributeValidation(entity.getId(), attrCode, validation)));
    }

    @Override
    @Transactional
    public Draft createFromVersion(Integer versionId) {

        versionValidation.validateVersion(versionId);
        RefBookVersionEntity sourceVersion = versionRepository.getOne(versionId);

        Map<String, Object> passport = new HashMap<>();
        sourceVersion.getPassportValues().forEach(passportValueEntity -> passport.put(passportValueEntity.getAttribute().getCode(), passportValueEntity.getValue()));

        Map<String, List<AttributeValidation>> validations = attributeValidationRepository.findAllByVersionId(versionId).stream()
                .collect(groupingBy(AttributeValidationEntity::getAttribute, mapping(entity -> entity.getType().getValidationInstance().valueFromString(entity.getValue()), toList())));
        CreateDraftRequest draftRequest = new CreateDraftRequest(sourceVersion.getRefBook().getId(), sourceVersion.getStructure(), passport, validations);
        Draft draft = create(draftRequest);

        draftDataService.loadData(draft.getStorageCode(), sourceVersion.getStorageCode(), sourceVersion.getFromDate(), sourceVersion.getToDate());
        conflictRepository.copyByReferrerVersion(versionId, draft.getId());

        return draft;
    }

    /** Пересоздание черновика при его наличии. */
    private RefBookVersionEntity recreateDraft(Structure structure, RefBookVersionEntity draftVersion,
                                               List<Field> fields, List<PassportValueEntity> passportValues) {

        if (!structure.equals(draftVersion.getStructure())) {
            Integer refBookId = draftVersion.getRefBook().getId();

            if (passportValues == null) passportValues = draftVersion.getPassportValues();

            removeDraft(draftVersion);

            draftVersion = newDraftVersion(structure, passportValues);
            draftVersion.setRefBook(newRefBook(refBookId));
            String draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);

        } else {
            passportValueRepository.deleteInBatch(draftVersion.getPassportValues());
            deleteDraftAllRows(draftVersion);

            if (passportValues != null) draftVersion.setPassportValues(passportValues);
        }

        return draftVersion;
    }

    private RefBookEntity newRefBook(Integer refBookId) {
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(refBookId);
        return refBookEntity;
    }

    private RefBookVersionEntity newDraftVersion(Structure structure, List<PassportValueEntity> passportValues) {

        RefBookVersionEntity draftVersion = new RefBookVersionEntity();
        draftVersion.setStatus(RefBookVersionStatus.DRAFT);
        draftVersion.setPassportValues(passportValues.stream()
                .map(v -> new PassportValueEntity(v.getAttribute(), v.getValue(), draftVersion))
                .collect(toList()));
        draftVersion.setStructure(structure);
        return draftVersion;
    }

    private RefBookVersionEntity getDraftByRefBook(Integer refBookId) {
        return versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookId);
    }

    @Override
    @Transactional
    public void updateData(Integer draftId, List<Row> rows, Integer optLockValue) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);

        List<Object> addedData = null;
        List<RowDiff> updateDiffData = null;
        refBookLockService.setRefBookUpdating(draftEntity.getRefBook().getId());
        try {
            versionValidation.validateOptLockValue(draftId, draftEntity.getOptLockValue(), optLockValue);

            rows = prepareRows(rows, draftEntity, true);
            if (rows.isEmpty()) return;

            List<RowValue> rowValues = rows.stream().map(row -> ConverterUtil.rowValue((row), draftEntity.getStructure())).collect(toList());
            validateDataByStructure(draftEntity, rows);

            List<RowValue> addedRowValues = rowValues.stream().filter(rowValue -> rowValue.getSystemId() == null).collect(toList());
            if (!isEmpty(addedRowValues)) {
                try {
                    draftDataService.addRows(draftEntity.getStorageCode(), addedRowValues);

                } catch (RuntimeException e) {
                    ErrorUtil.rethrowError(e);
                }

                addedData = addedRowValues.stream().map(RowValue::getFieldValues).collect(toList());
            }

            List<RowValue> updatedRowValues = rowValues.stream().filter(rowValue -> rowValue.getSystemId() != null).collect(toList());
            if (!isEmpty(updatedRowValues)) {
                List<String> fields = StructureUtils.getAttributeCodes(draftEntity.getStructure()).collect(toList());
                List<Object> systemIds = updatedRowValues.stream().map(RowValue::getSystemId).collect(toList());
                List<RowValue> oldRowValues = searchDataService.findRows(draftEntity.getStorageCode(), fields, systemIds);

                List<Message> messages = systemIds.stream()
                        .filter(systemId -> !RowUtils.isSystemIdRowValue(systemId, oldRowValues))
                        .map(systemId -> new Message(ROW_NOT_FOUND_EXCEPTION_CODE, systemId))
                        .collect(toList());
                if (!isEmpty(messages)) throw new UserException(messages);

                updateDiffData = oldRowValues.stream()
                        .map(oldRowValue -> {
                            RowValue newRowValue = RowUtils.getSystemIdRowValue(oldRowValue.getSystemId(), updatedRowValues);
                            return RowDiffUtils.getRowDiff(oldRowValue, newRowValue);
                        })
                        .collect(toList());

                conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(draftEntity.getId(), RowUtils.toLongSystemIds(systemIds));
                try {
                    draftDataService.updateRows(draftEntity.getStorageCode(), updatedRowValues);

                } catch (RuntimeException e) {
                    ErrorUtil.rethrowError(e);
                }
            }

            if (!isEmpty(addedRowValues) || !isEmpty(updatedRowValues)) {
                forceUpdateOptLockValue(draftEntity);
            }

        } finally {
            refBookLockService.deleteRefBookOperation(draftEntity.getRefBook().getId());
        }

        auditEditData(draftEntity, Map.of(
                "create_rows", addedData == null ? "-" : addedData,
                "update_rows", updateDiffData == null ? "-" : updateDiffData
        ));
    }

    @Override
    @Transactional
    public void deleteRows(Integer draftId, List<Row> rows, Integer optLockValue) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);

        List<Object> systemIds;
        refBookLockService.setRefBookUpdating(draftEntity.getRefBook().getId());
        try {
            versionValidation.validateOptLockValue(draftId, draftEntity.getOptLockValue(), optLockValue);

            rows = prepareRows(rows, draftEntity, false);
            if (rows.isEmpty()) return;

            systemIds = rows.stream().map(Row::getSystemId).filter(Objects::nonNull).collect(toList());
            if (!systemIds.isEmpty()) {
                conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(draftEntity.getId(), RowUtils.toLongSystemIds(systemIds));
                draftDataService.deleteRows(draftEntity.getStorageCode(), systemIds);
                forceUpdateOptLockValue(draftEntity);
            }
        } finally {
            refBookLockService.deleteRefBookOperation(draftEntity.getRefBook().getId());
        }

        auditEditData(draftEntity, "delete_rows", systemIds);
    }

    /** Подготовка записей к добавлению/обновлению/удалению. */
    private List<Row> prepareRows(List<Row> rows, RefBookVersionEntity draftVersion, boolean excludeEmptyRows) {

        if (isEmpty(rows)) return emptyList();

        Set<String> attributeCodes = StructureUtils.getAttributeCodes(draftVersion.getStructure()).collect(toSet());

        // Исключение полей, не соответствующих атрибутам структуры
        rows.forEach(row -> row.getData().entrySet().removeIf(entry -> !attributeCodes.contains(entry.getKey())));

        if (excludeEmptyRows) {
            rows = rows.stream().filter(row -> !RowUtils.isEmptyRow(row)).collect(toList());
        }

        if (isEmpty(rows)) return emptyList();

        validateDataByType(draftVersion.getStructure(), rows);
        fillSystemIdsByPrimaries(draftVersion, rows);

        return rows;
    }

    /** Валидация добавляемых/обновляемых строк данных по типу. */
    private void validateDataByType(Structure structure, List<Row> rows) {

        NonStrictOnTypeRowMapper mapper = new NonStrictOnTypeRowMapper(structure, versionRepository);
        List<Message> errors = rows.stream()
                .map(mapper::map)
                .map(row -> new TypeValidation(row.getData(), structure).validate())
                .filter(list -> !isEmpty(list))
                .flatMap(Collection::stream).collect(toList());

        if (!isEmpty(errors))
            throw new UserException(errors);
    }

    /** Заполнение systemId из имеющихся записей, совпадающих по первичным ключам. */
    private void fillSystemIdsByPrimaries(RefBookVersionEntity draftVersion, List<Row> rows) {

        List<Structure.Attribute> primaries = draftVersion.getStructure().getPrimary();
        if (primaries.isEmpty()) return;

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(rows.size());

        List<AttributeFilter> filters = rows.stream()
                .filter(row -> row.getSystemId() == null)
                .map(row -> RowUtils.getPrimaryKeyValueFilters(row, primaries))
                .flatMap(Collection::stream).collect(toList());
        criteria.setAttributeFilter(Set.of(filters));

        Page<RefBookRowValue> rowValues = versionService.search(draftVersion.getId(), criteria);
        if (rowValues == null || isEmpty(rowValues.getContent()))
            return;

        rowValues.getContent().forEach(refBookRowValue ->
                rows.stream()
                        .filter(row -> row.getSystemId() == null)
                        .filter(row -> RowUtils.equalsValuesByAttributes(row, refBookRowValue, primaries))
                        .forEach(row -> row.setSystemId(refBookRowValue.getSystemId()))
        );
    }

    /** Валидация добавляемых/обновляемых строк данных по структуре. */
    private void validateDataByStructure(RefBookVersionEntity draftVersion, List<Row> rows) {

        if (isEmpty(rows)) return;

        RowsValidator validator = new RowsValidatorImpl(versionService, searchDataService,
                draftVersion.getStructure(), draftVersion.getStorageCode(), errorCountLimit, false,
                attributeValidationRepository.findAllByVersionId(draftVersion.getId()));
        rows.forEach(validator::append);
        validator.process();
    }

    @Override
    @Transactional
    public void deleteAllRows(Integer draftId, Integer optLockValue) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);

        refBookLockService.setRefBookUpdating(draftEntity.getRefBook().getId());
        try {
            versionValidation.validateOptLockValue(draftId, draftEntity.getOptLockValue(), optLockValue);

            deleteDraftAllRows(draftEntity);
            forceUpdateOptLockValue(draftEntity);

        } finally {
            refBookLockService.deleteRefBookOperation(draftEntity.getRefBook().getId());
        }

        auditEditData(draftEntity, "delete_all_rows", "-");
    }

    /** Удаление всех строк черновика. */
    private void deleteDraftAllRows(RefBookVersionEntity draftVersion) {

        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftVersion.getId());
        draftDataService.deleteAllRows(draftVersion.getStorageCode());
    }

    @Override
    // @SuppressWarnings({"squid:S1166", "squid:S00108"})
    public void updateData(Integer draftId, FileModel fileModel, Integer optLockValue) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftEntity = versionRepository.findById(draftId).orElseThrow();

        Integer refBookId = draftEntity.getRefBook().getId();
        refBookLockService.setRefBookUpdating(refBookId);
        try {
            updateDraftData(draftEntity, fileModel);
            forceUpdateOptLockValue(versionRepository.findById(draftId).orElse(null));

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        // Нельзя просто передать draftEntity, так как в аудите подтягиваются значения паспорта справочника
        // (а у них lazy-инициализация), поэтому нужна транзакция (которой в этом методе нет).
        auditLogService.addAction(AuditAction.UPLOAD_DATA, () -> versionRepository.findById(draftId).orElse(null));
    }

    @Override
    @Transactional
    public Page<RefBookRowValue> search(Integer draftId, SearchDataCriteria criteria) {

        versionValidation.validateDraftExists(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        String storageCode = draft.getStorageCode();
        List<Field> fields = ConverterUtil.fields(draft.getStructure());

        DataCriteria dataCriteria = new DataCriteria(storageCode, null, null,
                fields, ConverterUtil.getFieldSearchCriteriaList(criteria.getAttributeFilter()), criteria.getCommonFilter());
        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return new RowValuePage(pagedData).map(rv -> new RefBookRowValue((LongRowValue) rv, draft.getId()));
    }

    @Override
    @Transactional
    public Boolean hasData(Integer draftId) {

        versionValidation.validateDraftExists(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        return searchDataService.hasData(draft.getStorageCode());
    }

    private RefBookVersionEntity getLastRefBookVersion(Integer refBookId) {
        return versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(refBookId, RefBookVersionStatus.PUBLISHED);
    }

    @Override
    @Transactional
    public void remove(Integer draftId) {

        versionValidation.validateDraft(draftId);
        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);
        removeDraft(draftVersion);
    }

    /** Удаление черновика. */
    public void removeDraft(RefBookVersionEntity draftVersion) {

        dropDataService.drop(singleton(draftVersion.getStorageCode()));
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftVersion.getId());
        versionRepository.deleteById(draftVersion.getId());
    }

    @Override
    @Transactional
    public Draft getDraft(Integer draftId) {

        versionValidation.validateDraftExists(draftId);
        return versionRepository.getOne(draftId).toDraft();
    }

    @Override
    public Draft findDraft(String refBookCode) {

        RefBookVersionEntity draftEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.DRAFT);
        return draftEntity != null ? draftEntity.toDraft() : null;
    }

    @Override
    @Transactional
    public void createAttribute(CreateAttribute createAttribute, Integer optLockValue) {

        final Integer draftId = createAttribute.getVersionId();
        versionValidation.validateDraft(draftId);
        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);
        versionValidation.validateOptLockValue(draftId, draftEntity.getOptLockValue(), optLockValue);

        Structure structure = draftEntity.getStructure();
        if (structure == null)
            structure = new Structure();

        structureChangeValidator.validateCreateAttribute(createAttribute);

        Structure.Attribute attribute = createAttribute.getAttribute();
        validateNewAttribute(attribute, structure, draftEntity.getRefBook().getCode());

        Structure.Reference reference = createAttribute.getReference();
        if (reference != null && reference.isNull())
            reference = null;

        if (reference != null) {
            validateNewReference(attribute, reference, structure, draftEntity.getRefBook().getCode());
        }

        structureChangeValidator.validateCreateAttributeStorage(attribute, structure, draftEntity.getStorageCode());

        try {
            draftDataService.addField(draftEntity.getStorageCode(), ConverterUtil.field(attribute));

        } catch (CodifiedException ce) {
            throw new UserException(new Message(ce.getMessage(), ce.getArgs()), ce);
        }

        // Должен быть только один первичный ключ:
        if (attribute.hasIsPrimary())
            structure.clearPrimary();

        structure.add(attribute, reference);
        draftEntity.setStructure(structure);
        forceUpdateOptLockValue(draftEntity);

        auditStructureEdit(draftEntity, "create_attribute", attribute);
    }

    @Override
    @Transactional
    public void updateAttribute(UpdateAttribute updateAttribute, Integer optLockValue) {

        final Integer draftId = updateAttribute.getVersionId();
        versionValidation.validateDraft(draftId);
        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);
        versionValidation.validateOptLockValue(draftId, draftEntity.getOptLockValue(), optLockValue);

        Structure structure = draftEntity.getStructure();

        Structure.Attribute oldAttribute = structure.getAttribute(updateAttribute.getCode());
        structureChangeValidator.validateUpdateAttribute(updateAttribute, oldAttribute);

        Structure.Attribute newAttribute = Structure.Attribute.build(oldAttribute);
        updateAttribute.fillAttribute(newAttribute);
        validateNewAttribute(newAttribute, structure, draftEntity.getRefBook().getCode());

        Structure.Reference oldReference = structure.getReference(oldAttribute.getCode());
        Structure.Reference newReference = null;
        if (newAttribute.isReferenceType()) {
            newReference = Structure.Reference.build(oldReference);
            updateAttribute.fillReference(newReference);
            validateNewReference(newAttribute, newReference, structure, draftEntity.getRefBook().getCode());
        }

        structureChangeValidator.validateUpdateAttributeStorage(updateAttribute, oldAttribute, draftEntity.getStorageCode());

        try {
            draftDataService.updateField(draftEntity.getStorageCode(), ConverterUtil.field(newAttribute));

        } catch (CodifiedException ce) {
            throw new UserException(new Message(ce.getMessage(), ce.getArgs()), ce);
        }

        // Должен быть только один первичный ключ:
        if (newAttribute.hasIsPrimary())
            structure.clearPrimary();

        structure.update(oldAttribute, newAttribute);
        structure.update(oldReference, newReference);

        // Обновление значений ссылки только по необходимости:
        if (!StructureUtils.isDisplayExpressionEquals(oldReference, newReference)) {
            refreshReferenceDisplayValues(draftEntity, newReference);
        }

        // Валидации для старого типа удаляются отдельным вызовом updateAttributeValidations.
        if (Objects.equals(oldAttribute.getType(), updateAttribute.getType())) {
            attributeValidationRepository.deleteByVersionIdAndAttribute(draftId, updateAttribute.getCode());
        }

        forceUpdateOptLockValue(draftEntity);

        auditStructureEdit(draftEntity, "update_attribute", newAttribute);
    }

    private void validateNewAttribute(Structure.Attribute newAttribute,
                                      Structure oldStructure, String refBookCode) {

        if (!newAttribute.hasIsPrimary() && !newAttribute.isReferenceType()
                && !isEmpty(oldStructure.getReferences()) && !oldStructure.hasPrimary())
            throw new UserException(new Message(VersionValidationImpl.REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));

        versionValidation.validateAttribute(newAttribute);
    }

    private void validateNewReference(Structure.Attribute newAttribute,
                                      Structure.Reference newReference,
                                      Structure oldStructure, String refBookCode) {

        if (newAttribute.hasIsPrimary())
            throw new UserException(new Message(VersionValidationImpl.REFERENCE_ATTRIBUTE_CANNOT_BE_PRIMARY_KEY_EXCEPTION_CODE, newAttribute.getName()));

        if (!oldStructure.hasPrimary())
            throw new UserException(new Message(VersionValidationImpl.REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));

        Structure.Reference oldReference = oldStructure.getReference(newReference.getAttribute());
        if (!StructureUtils.isDisplayExpressionEquals(oldReference, newReference)) {
            versionValidation.validateReferenceAbility(newReference);
        }
    }

    /**
     * Обновление отображаемого значения ссылки во всех записях с заполненным значением ссылки.
     *
     * @param draftEntity сущность-черновик
     * @param reference   атрибут-ссылка
     */
    private void refreshReferenceDisplayValues(RefBookVersionEntity draftEntity, Structure.Reference reference) {

        if (reference == null) return;

        RefBookVersionEntity publishedEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(reference.getReferenceCode(), RefBookVersionStatus.PUBLISHED);
        if (publishedEntity == null) return;

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(publishedEntity.getStructure());
        if (referenceAttribute == null) return;

        Reference updatedReference = new Reference(
                publishedEntity.getStorageCode(),
                publishedEntity.getFromDate(), // SYS_PUBLISH_TIME is not exist for draft
                referenceAttribute.getCode(),
                new DisplayExpression(reference.getDisplayExpression()),
                null, // Old value is not changed
                null // Display value will be recalculated
        );
        ReferenceFieldValue fieldValue = new ReferenceFieldValue(reference.getAttribute(), updatedReference);

        draftDataService.updateReferenceInRefRows(draftEntity.getStorageCode(), fieldValue, null, null);
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftEntity.getId());
    }

    @Override
    @Transactional
    public void deleteAttribute(Integer draftId, String attributeCode, Integer optLockValue) {

        versionValidation.validateDraft(draftId);
        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);
        Structure structure = draftEntity.getStructure();

        Structure.Attribute attribute = structure.getAttribute(attributeCode);
        validateOldAttribute(attribute, structure, draftEntity.getRefBook().getCode());

        try {
            draftDataService.deleteField(draftEntity.getStorageCode(), attributeCode);

        } catch (RuntimeException e) {
            ErrorUtil.rethrowError(e);
        }

        structure.remove(attributeCode);

        attributeValidationRepository.deleteByVersionIdAndAttribute(draftId, attributeCode);

        forceUpdateOptLockValue(draftEntity);

        auditStructureEdit(draftEntity, "delete_attribute", attribute);
    }

    private void validateOldAttribute(Structure.Attribute oldAttribute,
                                      Structure oldStructure, String refBookCode) {

        if (oldAttribute.hasIsPrimary() && !isEmpty(oldStructure.getReferences()) && oldStructure.getPrimary().size() == 1)
            throw new UserException(new Message(VersionValidationImpl.REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));
    }

    @Override
    @Transactional
    public void addAttributeValidation(Integer versionId, String attribute, AttributeValidation attributeValidation) {

        versionValidation.validateDraftAttributeExists(versionId, attribute);

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        AttributeValidationEntity validationEntity = new AttributeValidationEntity(versionEntity, attribute,
                attributeValidation.getType(), attributeValidation.valuesToString());
        validateVersionData(versionEntity, false, singletonList(validationEntity));

        deleteAttributeValidation(versionId, attribute, attributeValidation.getType());
        attributeValidationRepository.save(validationEntity);
    }

    @Override
    @Transactional
    public void deleteAttributeValidation(Integer draftId, String attribute, AttributeValidationType type) {

        List<AttributeValidationEntity> validations;
        if (attribute == null) {
            versionValidation.validateDraftExists(draftId);
            validations = attributeValidationRepository.findAllByVersionId(draftId);

        } else {
            versionValidation.validateDraftAttributeExists(draftId, attribute);
            validations = (type == null)
                    ? attributeValidationRepository.findAllByVersionIdAndAttribute(draftId, attribute)
                    : attributeValidationRepository.findAllByVersionIdAndAttributeAndType(draftId, attribute, type);
        }

        if (!validations.isEmpty())
            attributeValidationRepository.deleteAll(validations);
    }

    @Override
    public List<AttributeValidation> getAttributeValidations(Integer draftId, String attribute) {

        List<AttributeValidationEntity> validationEntities = (attribute == null)
                ? attributeValidationRepository.findAllByVersionId(draftId)
                : attributeValidationRepository.findAllByVersionIdAndAttribute(draftId, attribute);
        return validationEntities.stream().map(AttributeValidationEntity::attributeValidationModel).collect(toList());
    }

    @Override
    @Transactional
    public void updateAttributeValidations(Integer versionId, AttributeValidationRequest request) {

        versionValidation.validateDraftExists(versionId);

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        updateAttributeValidations(versionEntity, request.getOldAttribute(), request.getNewAttribute(), request.getValidations());
    }

    private void updateAttributeValidations(RefBookVersionEntity versionEntity,
                                            RefBookVersionAttribute oldAttribute,
                                            RefBookVersionAttribute newAttribute,
                                            List<AttributeValidation> validations) {

        String attributeCode = newAttribute.getAttribute().getCode();
        Structure structure = versionEntity.getStructure();

        versionValidation.validateAttributeExists(versionEntity.getId(), structure, attributeCode);

        List<AttributeValidationEntity> validationEntities = validations.stream()
                .map(validation -> new AttributeValidationEntity(versionEntity, attributeCode, validation.getType(), validation.valuesToString()))
                .collect(toList());

        boolean skipReferenceValidation = isReferenceValidationSkipped(versionEntity.getId(), oldAttribute, newAttribute);
        validateVersionData(versionEntity, skipReferenceValidation, validationEntities);

        deleteAttributeValidation(versionEntity.getId(), attributeCode, null);

        attributeValidationRepository.saveAll(validationEntities);

        conflictRepository.deleteByReferrerVersionIdAndRefFieldCodeAndRefRecordIdIsNull(versionEntity.getId(), attributeCode);
    }

    /**
     * Возможность отключения валидации данных для ссылочных значений
     * из-за отсутствия ссылок, различия выражений или наличия конфликтов по структуре.
     */
    private boolean isReferenceValidationSkipped(Integer versionId,
                                                 RefBookVersionAttribute oldAttribute,
                                                 RefBookVersionAttribute newAttribute) {
        if (newAttribute.hasReference() && oldAttribute != null && oldAttribute.hasReference()) {
            if (newAttribute.equalsReferenceDisplayExpression(oldAttribute))
                return false;

            return BooleanUtils.isTrue(
                    conflictRepository.hasReferrerConflict(versionId, newAttribute.getAttribute().getCode(),
                            ConflictType.DISPLAY_DAMAGED, RefBookVersionStatus.PUBLISHED)
            );
        }

        return false;
    }

    private void validateVersionData(RefBookVersionEntity versionEntity,
                                     boolean skipReferenceValidation,
                                     List<AttributeValidationEntity> validationEntities) {

        VersionDataIterator iterator = new VersionDataIterator(versionService, singletonList(versionEntity.getId()));
        RowsValidator validator = new RowsValidatorImpl(versionService, searchDataService,
                versionEntity.getStructure(), versionEntity.getStorageCode(),
                errorCountLimit, skipReferenceValidation, validationEntities
        );

        while (iterator.hasNext()) {
            validator.append(iterator.next());
        }
        validator.process();
    }

    /** Принудительное обновление значения оптимистической блокировки версии. */
    private void forceUpdateOptLockValue(RefBookVersionEntity entity) {

        if (entity == null)
            return;

        try {
            entity.setLastActionDate(TimeUtils.now());
            versionRepository.save(entity);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new UserException(OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE, e);
        }
    }

    @Override
    @Transactional
    public ExportFile getDraftFile(Integer draftId, FileType fileType) {

        versionValidation.validateDraftExists(draftId);

        RefBookVersion versionModel = ModelGenerator.versionModel(versionRepository.getOne(draftId));
        VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(versionModel.getId()));

        return new ExportFile(
                versionFileService.generate(versionModel, fileType, dataIterator),
                fileNameGenerator.generateZipName(versionModel, fileType));
    }

    private void auditStructureEdit(RefBookVersionEntity refBook, String action, Structure.Attribute attribute) {
        auditLogService.addAction(AuditAction.EDIT_STRUCTURE, () -> refBook, Map.of(action, attribute));
    }

    private void auditEditData(RefBookVersionEntity refBook, String action, Object payload) {
        auditEditData(refBook, Map.of(action, payload));
    }

    private void auditEditData(RefBookVersionEntity refBook, Map<String, Object> payload) {
        auditLogService.addAction(AuditAction.DRAFT_EDITING, () -> refBook, payload);
    }
}