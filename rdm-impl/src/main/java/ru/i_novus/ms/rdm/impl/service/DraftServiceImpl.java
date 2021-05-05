package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.components.common.exception.CodifiedException;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.*;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.i_novus.ms.rdm.api.model.version.*;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.i_novus.ms.rdm.impl.file.process.*;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.draft.CreateDraftEntityStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.ValidateDraftExistsStrategy;
import ru.i_novus.ms.rdm.impl.strategy.file.ExportDraftFileStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.util.*;
import ru.i_novus.ms.rdm.impl.util.mappers.*;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.ms.rdm.impl.validation.TypeValidation;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.BaseDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.RowUtils.toLongSystemIds;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;
import static ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE;

@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class DraftServiceImpl implements DraftService {

    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";
    public static final String FILE_CONTENT_INVALID_EXCEPTION_CODE = "file.content.invalid";
    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private final RefBookVersionRepository versionRepository;
    private final RefBookConflictRepository conflictRepository;

    private final DraftDataService draftDataService;
    private final DropDataService dropDataService;
    private final SearchDataService searchDataService;

    private final RefBookLockService refBookLockService;
    private final VersionService versionService;

    private final FileStorage fileStorage;

    private final VersionValidation versionValidation;

    private final PassportValueRepository passportValueRepository;
    private final AttributeValidationRepository attributeValidationRepository;
    private final StructureChangeValidator structureChangeValidator;

    private final AuditLogService auditLogService;

    private final StrategyLocator strategyLocator;

    private int errorCountLimit = 100;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public DraftServiceImpl(RefBookVersionRepository versionRepository, RefBookConflictRepository conflictRepository,
                            DraftDataService draftDataService, DropDataService dropDataService,
                            SearchDataService searchDataService,
                            RefBookLockService refBookLockService, VersionService versionService,
                            FileStorage fileStorage,
                            VersionValidation versionValidation,
                            PassportValueRepository passportValueRepository,
                            AttributeValidationRepository attributeValidationRepository,
                            StructureChangeValidator structureChangeValidator,
                            AuditLogService auditLogService,
                            StrategyLocator strategyLocator) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
        this.searchDataService = searchDataService;

        this.refBookLockService = refBookLockService;
        this.versionService = versionService;

        this.fileStorage = fileStorage;

        this.versionValidation = versionValidation;

        this.passportValueRepository = passportValueRepository;
        this.attributeValidationRepository = attributeValidationRepository;
        this.structureChangeValidator = structureChangeValidator;

        this.auditLogService = auditLogService;

        this.strategyLocator = strategyLocator;
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
            draft = createFromFile(refBookId, fileModel);

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        auditLogService.addAction(AuditAction.UPLOAD_VERSION_FROM_FILE, () -> versionRepository.getOne(draft.getId()));

        return draft;
    }

    /** Создание и обновление данных черновика справочника из файла. */
    private Draft createFromFile(Integer refBookId, FileModel fileModel) {

        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());

        return switch (FileUtil.getExtension(fileModel.getName())) {
            case "XLSX" -> createFromXlsx(refBookId, fileModel, inputStreamSupplier);
            case "XML" -> createFromXml(refBookId, fileModel, inputStreamSupplier);
            default -> throw new FileExtensionException();
        };
    }

    private Draft createFromXlsx(Integer refBookId, FileModel fileModel,
                                 Supplier<InputStream> inputStreamSupplier) {

        String extension = FileUtil.getExtension(fileModel.getName());
        BiConsumer<String, Structure> saveDraftConsumer = getSaveDraftConsumer(refBookId);
        RowsProcessor rowsProcessor = new CreateDraftBufferedRowsPersister(draftDataService, this::createDraftStorage, saveDraftConsumer);
        processFileRows(extension, rowsProcessor, new PlainRowMapper(), inputStreamSupplier);

        return findDraftEntity(refBookId).toDraft();
    }

    private Draft createFromXml(Integer refBookId, FileModel fileModel,
                                Supplier<InputStream> inputStreamSupplier) {

        try (XmlUpdateDraftFileProcessor xmlUpdateDraftFileProcessor = new XmlUpdateDraftFileProcessor(refBookId, this)) {
            Draft draft = xmlUpdateDraftFileProcessor.process(inputStreamSupplier);
            updateDataFromFile(versionRepository.getOne(draft.getId()), fileModel);
            return draft;
        }
    }

    /** Обновление данных черновика из файла. */
    private void updateDataFromFile(RefBookVersionEntity draft, FileModel fileModel) {

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

            RefBookVersionEntity publishedEntity = findLastPublishedEntity(refBookId);
            RefBookVersionEntity draftEntity = getStrategy(publishedEntity, ValidateDraftExistsStrategy.class).isDraft(publishedEntity)
                    ? publishedEntity
                    : findDraftEntity(refBookId);
            if (draftEntity == null && publishedEntity == null)
                throw new NotFoundException(new Message(VersionValidationImpl.REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

            final String refBookCode = (draftEntity != null ? draftEntity : publishedEntity).getRefBook().getCode();
            versionValidation.validateDraftStructure(refBookCode, structure);
            // Валидация структуры ссылочного справочника не нужна, т.к. все атрибуты строковые.

            if (draftEntity == null) {
                draftEntity = createDraftEntity(publishedEntity.getRefBook(), structure, publishedEntity.getPassportValues());

            } else if (draftEntity.hasEmptyStructure()) {
                draftEntity.setStructure(structure);

            } else {
                removeDraft(draftEntity);
                versionRepository.flush(); // Delete old draft before insert new draft!

                draftEntity = createDraftEntity(draftEntity.getRefBook(), structure, draftEntity.getPassportValues());
            }

            draftEntity.setStorageCode(storageCode);

            versionRepository.save(draftEntity);
        };
    }

    @Override
    @Transactional
    public Draft create(CreateDraftRequest request) {

        final Integer refBookId = request.getRefBookId();
        versionValidation.validateRefBook(refBookId);

        RefBookVersionEntity publishedEntity = findLastPublishedEntity(refBookId);
        RefBookVersionEntity draftEntity = getStrategy(publishedEntity, ValidateDraftExistsStrategy.class).isDraft(publishedEntity)
                ? publishedEntity
                : findDraftEntity(refBookId);
        if (draftEntity == null && publishedEntity == null)
            throw new NotFoundException(new Message(VersionValidationImpl.REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

        List<PassportValueEntity> passportValues = null;
        if (request.getPassport() != null) {
            passportValues = RefBookVersionEntity.toPassportValues(request.getPassport(), true, null);
        }

        final String refBookCode = (draftEntity != null ? draftEntity : publishedEntity).getRefBook().getCode();
        final Structure structure = request.getStructure();

        versionValidation.validateDraftStructure(refBookCode, structure);
        if (request.getReferrerValidationRequired())
            versionValidation.validateReferrerStructure(structure);

        if (draftEntity == null) {
            if (passportValues == null) passportValues = publishedEntity.getPassportValues();
            draftEntity = createDraftEntity(publishedEntity.getRefBook(), structure, passportValues);

            String draftCode = createDraftStorage(structure);
            draftEntity.setStorageCode(draftCode);

        } else {
            draftEntity = recreateDraft(draftEntity, structure, passportValues);
        }

        RefBookVersionEntity savedDraftEntity = versionRepository.save(draftEntity);
        addValidations(request.getValidations(), savedDraftEntity);

        return savedDraftEntity.toDraft();
    }

    private void addValidations(Map<String, List<AttributeValidation>> validations, RefBookVersionEntity entity) {

        if (!isEmpty(validations))
            validations.forEach((attrCode, list) ->
                    list.forEach(validation -> addAttributeValidation(entity.getId(), attrCode, validation))
            );
    }

    @Override
    @Transactional
    public Draft createFromVersion(Integer versionId) {

        RefBookVersionEntity versionEntity = findVersionOrThrow(versionId);
        getStrategy(versionEntity, ValidateVersionNotArchivedStrategy.class).validate(versionEntity);

        if (getStrategy(versionEntity, ValidateDraftExistsStrategy.class).isDraft(versionEntity))
            return new Draft(versionEntity.getId(), versionEntity.getStorageCode(), versionEntity.getOptLockValue());

        Map<String, Object> passport = new HashMap<>();
        versionEntity.getPassportValues().forEach(passportValueEntity -> passport.put(passportValueEntity.getAttribute().getCode(), passportValueEntity.getValue()));

        Map<String, List<AttributeValidation>> validations = attributeValidationRepository.findAllByVersionId(versionId).stream()
                .collect(groupingBy(AttributeValidationEntity::getAttribute, mapping(entity -> entity.getType().getValidationInstance().valueFromString(entity.getValue()), toList())));
        CreateDraftRequest draftRequest = new CreateDraftRequest(versionEntity.getRefBook().getId(), versionEntity.getStructure(), passport, validations);
        Draft draft = create(draftRequest);

        if (!versionEntity.getId().equals(draft.getId())) {
            draftDataService.loadData(draft.getStorageCode(), versionEntity.getStorageCode(), versionEntity.getFromDate(), versionEntity.getToDate());
            conflictRepository.copyByReferrerVersion(versionId, draft.getId());
        }

        return draft;
    }

    /** Пересоздание черновика при его наличии. */
    private RefBookVersionEntity recreateDraft(RefBookVersionEntity draftEntity,
                                               Structure structure, List<PassportValueEntity> passportValues) {

        if (!structure.equals(draftEntity.getStructure())) {

            if (passportValues == null) passportValues = draftEntity.getPassportValues();

            removeDraft(draftEntity);

            draftEntity = createDraftEntity(draftEntity.getRefBook(), structure, passportValues);

            String draftCode = createDraftStorage(structure);
            draftEntity.setStorageCode(draftCode);

        } else {
            if (!isEmpty(draftEntity.getPassportValues())) {
                passportValueRepository.deleteInBatch(draftEntity.getPassportValues());
            }
            deleteDraftAllRows(draftEntity);

            if (!isEmpty(passportValues)) draftEntity.setPassportValues(passportValues);
        }

        return draftEntity;
    }

    private String createDraftStorage(Structure structure) {

        return draftDataService.createDraft(ConverterUtil.fields(structure));
    }

    private RefBookVersionEntity createDraftEntity(RefBookEntity refBookEntity, Structure structure,
                                                   List<PassportValueEntity> passportValues) {

        return getStrategy(refBookEntity.getType(), CreateDraftEntityStrategy.class)
                .create(refBookEntity, structure, passportValues);
    }

    private RefBookVersionEntity findLastPublishedEntity(Integer refBookId) {

        return versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(refBookId, RefBookVersionStatus.PUBLISHED);
    }

    private RefBookVersionEntity findDraftEntity(Integer refBookId) {

        return versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookId);
    }

    @Override
    @Transactional
    public void updateData(Integer draftId, UpdateDataRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        List<Object> addedData = null;
        List<RowDiff> updatedDiffData = null;
        refBookLockService.setRefBookUpdating(draftEntity.getRefBook().getId());
        try {
            validateOptLockValue(draftEntity, request);

            List<Row> rows = prepareRows(request.getRows(), draftEntity, true);
            if (rows.isEmpty()) return;

            validateDataByStructure(draftEntity, rows);

            List<RowValue> rowValues = rows.stream().map(row -> ConverterUtil.rowValue(row, draftEntity.getStructure())).collect(toList());

            List<RowValue> addedRowValues = rowValues.stream().filter(rowValue -> rowValue.getSystemId() == null).collect(toList());
            if (!isEmpty(addedRowValues)) {
                addRowValues(draftEntity, addedRowValues);
                addedData = getAddedData(rowValues);
            }

            List<RowValue> updatedRowValues = rowValues.stream().filter(rowValue -> rowValue.getSystemId() != null).collect(toList());
            if (!isEmpty(updatedRowValues)) {
                updatedDiffData = getUpdatedDiffData(draftEntity, updatedRowValues);
                updateRowValues(draftEntity, updatedRowValues);
            }

            if (!isEmpty(addedRowValues) || !isEmpty(updatedRowValues)) {
                forceUpdateOptLockValue(draftEntity);
            }

        } finally {
            refBookLockService.deleteRefBookOperation(draftEntity.getRefBook().getId());
        }

        auditEditData(draftEntity, Map.of(
                "create_rows", isEmpty(addedData) ? "-" : addedData,
                "update_rows", isEmpty(updatedDiffData) ? "-" : updatedDiffData
        ));
    }

    private List<Object> getAddedData(List<RowValue> rowValues) {
        return rowValues.stream().map(RowValue::getFieldValues).collect(toList());
    }

    private void addRowValues(RefBookVersionEntity entity, List<RowValue> rowValues) {
        try {
            draftDataService.addRows(entity.getStorageCode(), rowValues);

        } catch (RuntimeException e) {
            ErrorUtil.rethrowError(e);
        }
    }

    private List<RowDiff> getUpdatedDiffData(RefBookVersionEntity entity, List<RowValue> updatedRowValues) {

        List<String> fields = entity.getStructure().getAttributeCodes();
        List<Object> systemIds = RowUtils.toSystemIds(updatedRowValues);
        List<RowValue> oldRowValues = searchDataService.findRows(entity.getStorageCode(), fields, systemIds);

        List<Message> messages = systemIds.stream()
                .filter(systemId -> !RowUtils.isSystemIdRowValue(systemId, oldRowValues))
                .map(systemId -> new Message(ROW_NOT_FOUND_EXCEPTION_CODE, systemId))
                .collect(toList());
        if (!isEmpty(messages)) throw new UserException(messages);

        return oldRowValues.stream()
                .map(oldRowValue -> {
                    RowValue newRowValue = RowUtils.getSystemIdRowValue(oldRowValue.getSystemId(), updatedRowValues);
                    return RowDiffUtils.getRowDiff(oldRowValue, newRowValue);
                })
                .collect(toList());
    }

    private void updateRowValues(RefBookVersionEntity entity, List<RowValue> rowValues) {

        List<Object> systemIds = RowUtils.toSystemIds(rowValues);
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(entity.getId(), toLongSystemIds(systemIds));

        try {
            draftDataService.updateRows(entity.getStorageCode(), rowValues);

        } catch (RuntimeException e) {
            ErrorUtil.rethrowError(e);
        }
    }

    @Override
    @Transactional
    public void deleteData(Integer draftId, DeleteDataRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        List<Object> systemIds;
        refBookLockService.setRefBookUpdating(draftEntity.getRefBook().getId());
        try {
            validateOptLockValue(draftEntity, request);

            List<Row> rows = prepareRows(request.getRows(), draftEntity, false);
            if (rows.isEmpty()) return;

            systemIds = rows.stream().map(Row::getSystemId).filter(Objects::nonNull).collect(toList());
            if (!systemIds.isEmpty()) {
                deleteRows(draftEntity, systemIds);

                forceUpdateOptLockValue(draftEntity);
            }
        } finally {
            refBookLockService.deleteRefBookOperation(draftEntity.getRefBook().getId());
        }

        auditEditData(draftEntity, "delete_rows", systemIds);
    }

    private void deleteRows(RefBookVersionEntity entity, List<Object> systemIds) {

        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(entity.getId(), toLongSystemIds(systemIds));
        draftDataService.deleteRows(entity.getStorageCode(), systemIds);
    }

    /** Подготовка записей к добавлению/обновлению/удалению. */
    private List<Row> prepareRows(List<Row> rows, RefBookVersionEntity draftVersion, boolean excludeEmptyRows) {

        if (isEmpty(rows)) return emptyList();

        Set<String> attributeCodes = new HashSet<>(draftVersion.getStructure().getAttributeCodes());

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

        List<Structure.Attribute> primaries = draftVersion.getStructure().getPrimaries();
        if (primaries.isEmpty()) return;

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(rows.size());

        Set<List<AttributeFilter>> filterSet = rows.stream()
                .filter(row -> row.getSystemId() == null)
                .map(row -> RowUtils.getPrimaryKeyValueFilters(row, primaries))
                .filter(list -> !isEmpty(list))
                .collect(toSet());
        criteria.setAttributeFilters(filterSet);

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
    public void deleteAllData(Integer draftId, DeleteAllDataRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.setRefBookUpdating(draftEntity.getRefBook().getId());
        try {
            validateOptLockValue(draftEntity, request);

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
    public void updateFromFile(Integer draftId, UpdateFromFileRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        Integer refBookId = draftEntity.getRefBook().getId();
        refBookLockService.setRefBookUpdating(refBookId);
        try {
            updateDataFromFile(draftEntity, request.getFileModel());
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

        RefBookVersionEntity entity = findVersion(draftId);
        getStrategy(entity, ValidateDraftExistsStrategy.class).validate(entity, draftId);

        return getRowValuesOfDraft(entity, criteria);
    }

    private Page<RefBookRowValue> getRowValuesOfDraft(RefBookVersionEntity draft, SearchDataCriteria criteria) {

        List<Field> fields = makeOutputFields(draft, criteria.getLocaleCode());

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getAttributeFilters()));
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getPlainAttributeFilters(), draft.getStructure()));

        String storageCode = toStorageCode(draft, criteria);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(storageCode, null, null,
                fields, fieldSearchCriterias, criteria.getCommonFilter());

        dataCriteria.setPage(criteria.getPageNumber() + BaseDataCriteria.PAGE_SHIFT);
        dataCriteria.setSize(criteria.getPageSize());
        Optional.ofNullable(criteria.getSort()).ifPresent(sort -> dataCriteria.setSortings(ConverterUtil.sortings(sort)));

        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return new RowValuePage(pagedData).map(rv -> new RefBookRowValue((LongRowValue) rv, draft.getId()));
    }

    @Override
    @Transactional
    public Boolean hasData(Integer draftId) {

        RefBookVersionEntity entity = findVersion(draftId);
        getStrategy(entity, ValidateDraftExistsStrategy.class).validate(entity, draftId);

        return searchDataService.hasData(entity.getStorageCode());
    }

    @Override
    @Transactional
    public void remove(Integer draftId) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.validateRefBookNotBusy(draftEntity.getRefBook().getId());
        removeDraft(draftEntity);
    }

    /**
     * Удаление черновика.
     */
    private void removeDraft(RefBookVersionEntity draftEntity) {

        dropDataService.drop(singleton(draftEntity.getStorageCode()));
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftEntity.getId());
        versionRepository.deleteById(draftEntity.getId());
    }

    @Override
    @Transactional
    public Draft getDraft(Integer draftId) {

        RefBookVersionEntity entity = findVersion(draftId);
        getStrategy(entity, ValidateDraftExistsStrategy.class).validate(entity, draftId);

        return entity.toDraft();
    }

    // RDM-827: Задать стратегию для неверсионного справочника.
    @Override
    public Draft findDraft(String refBookCode) {

        RefBookVersionEntity draftEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.DRAFT);
        return draftEntity != null ? draftEntity.toDraft() : null;
    }

    @Override
    @Transactional
    public void createAttribute(Integer draftId, CreateAttributeRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.validateRefBookNotBusy(draftEntity.getRefBook().getId());
        validateOptLockValue(draftEntity, request);

        Structure structure = draftEntity.getStructure();
        if (structure == null)
            structure = new Structure();

        structureChangeValidator.validateCreateAttribute(request, structure);

        Structure.Attribute attribute = request.getAttribute();
        validateNewAttribute(attribute, structure, draftEntity.getRefBook().getCode());

        Structure.Reference reference = request.getReference();
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
    public void updateAttribute(Integer draftId, UpdateAttributeRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.validateRefBookNotBusy(draftEntity.getRefBook().getId());
        validateOptLockValue(draftEntity, request);

        Structure structure = draftEntity.getStructure();

        Structure.Attribute oldAttribute = structure.getAttribute(request.getCode());
        structureChangeValidator.validateUpdateAttribute(draftId, request, oldAttribute);

        Structure.Attribute newAttribute = Structure.Attribute.build(oldAttribute);
        request.fillAttribute(newAttribute);
        validateNewAttribute(newAttribute, structure, draftEntity.getRefBook().getCode());

        Structure.Reference oldReference = structure.getReference(oldAttribute.getCode());
        Structure.Reference newReference = null;
        if (newAttribute.isReferenceType()) {
            newReference = Structure.Reference.build(oldReference);
            request.fillReference(newReference);
            validateNewReference(newAttribute, newReference, structure, draftEntity.getRefBook().getCode());
        }

        structureChangeValidator.validateUpdateAttributeStorage(draftId, request, oldAttribute, draftEntity.getStorageCode());

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
        if (Objects.equals(oldAttribute.getType(), request.getType())) {
            attributeValidationRepository.deleteByVersionIdAndAttribute(draftId, request.getCode());
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
    public void deleteAttribute(Integer draftId, DeleteAttributeRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.validateRefBookNotBusy(draftEntity.getRefBook().getId());
        validateOptLockValue(draftEntity, request);

        Structure structure = draftEntity.getStructure();
        final String attributeCode = request.getAttributeCode();

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

        if (oldAttribute.hasIsPrimary() && !isEmpty(oldStructure.getReferences()) && oldStructure.getPrimaries().size() == 1)
            throw new UserException(new Message(VersionValidationImpl.REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));
    }

    @Override
    @Transactional
    public void addAttributeValidation(Integer draftId, String attribute, AttributeValidation attributeValidation) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        versionValidation.validateDraftAttributeExists(draftId, draftEntity.getStructure(), attribute);

        AttributeValidationEntity validationEntity = new AttributeValidationEntity(draftEntity, attribute,
                attributeValidation.getType(), attributeValidation.valuesToString());
        validateVersionData(draftEntity, false, singletonList(validationEntity));

        deleteAttributeValidation(draftId, attribute, attributeValidation.getType());
        attributeValidationRepository.save(validationEntity);
    }

    @Override
    @Transactional
    public void deleteAttributeValidation(Integer draftId, String attribute, AttributeValidationType type) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        List<AttributeValidationEntity> validations;
        if (attribute == null) {
            validations = attributeValidationRepository.findAllByVersionId(draftId);

        } else {
            versionValidation.validateDraftAttributeExists(draftId, draftEntity.getStructure(), attribute);
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
        return validationEntities.stream().map(AttributeValidationEntity::toModel).collect(toList());
    }

    @Override
    @Transactional
    public void updateAttributeValidations(Integer draftId, AttributeValidationRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        updateAttributeValidations(draftEntity, request.getOldAttribute(), request.getNewAttribute(), request.getValidations());
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

            return Boolean.TRUE.equals(
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
            entity.refreshLastActionDate();
            versionRepository.save(entity);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new UserException(OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE, e);
        }
    }

    @Override
    @Transactional
    public ExportFile getDraftFile(Integer draftId, FileType fileType) {

        RefBookVersionEntity entity = findVersion(draftId);
        getStrategy(entity, ValidateDraftExistsStrategy.class).validate(entity, draftId);

        RefBookVersion version = ModelGenerator.versionModel(entity);

        ExportFile exportFile = getStrategy(entity, ExportDraftFileStrategy.class)
                .export(version, fileType, versionService);

        auditLogService.addAction(AuditAction.DOWNLOAD, () -> entity);

        return exportFile;
    }

    private void validateOptLockValue(RefBookVersionEntity entity, DraftChangeRequest request) {
        versionValidation.validateOptLockValue(entity.getId(), entity.getOptLockValue(), request.getOptLockValue());
    }

    protected RefBookVersionEntity findForUpdate(Integer id) {

        RefBookVersionEntity entity = findVersion(id);
        getStrategy(entity, ValidateDraftExistsStrategy.class).validate(entity, id);
        getStrategy(entity, ValidateVersionNotArchivedStrategy.class).validate(entity);

        return entity;
    }

    private RefBookVersionEntity findVersionOrThrow(Integer id) {

        RefBookVersionEntity entity = findVersion(id);
        if (entity == null)
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, id));

        return entity;
    }

    protected RefBookVersionEntity findVersion(Integer id) {

        return (id != null) ? versionRepository.findById(id).orElse(null) : null;
    }

    private <T extends Strategy> T getStrategy(RefBookVersionEntity entity, Class<T> strategy) {

        return getStrategy(entity != null ? entity.getRefBook().getType() : null, strategy);
    }

    private <T extends Strategy> T getStrategy(RefBookTypeEnum type, Class<T> strategy) {

        return strategyLocator.getStrategy(type, strategy);
    }

    /**
     * Формирование списка полей, выводимых в результате запроса данных в хранилище версии.
     *
     * @param version    версия справочника
     * @param localeCode код локали
     * @return Список выводимых полей
     */
    @SuppressWarnings("UnusedParameter")
    protected List<Field> makeOutputFields(RefBookVersionEntity version, String localeCode) {

        return ConverterUtil.fields(version.getStructure());
    }

    /**
     * Преобразование кода хранилища с учётом локали.
     *
     * @param draft    черновик
     * @param criteria критерий поиска
     * @return Код хранилища с учётом локали
     */
    @SuppressWarnings("UnusedParameter")
    protected String toStorageCode(RefBookVersionEntity draft, SearchDataCriteria criteria) {
        return draft.getStorageCode();
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