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
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.FileExtensionException;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.i_novus.ms.rdm.api.model.version.*;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.i_novus.ms.rdm.impl.file.process.*;
import ru.i_novus.ms.rdm.impl.model.RefBookVersionEntityKit;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.data.*;
import ru.i_novus.ms.rdm.impl.strategy.draft.CreateDraftEntityStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.CreateDraftStorageStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.FindDraftEntityStrategy;
import ru.i_novus.ms.rdm.impl.strategy.structure.CreateAttributeStrategy;
import ru.i_novus.ms.rdm.impl.strategy.structure.DeleteAttributeStrategy;
import ru.i_novus.ms.rdm.impl.strategy.structure.UpdateAttributeStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.util.*;
import ru.i_novus.ms.rdm.impl.util.mappers.NonStrictOnTypeRowMapper;
import ru.i_novus.ms.rdm.impl.util.mappers.PlainRowMapper;
import ru.i_novus.ms.rdm.impl.util.mappers.StructureRowMapper;
import ru.i_novus.ms.rdm.impl.validation.TypeValidation;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.BaseDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;
import static ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE;

@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class DraftServiceImpl implements DraftService {

    private static final String VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE = "version.has.not.structure";
    private static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";
    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";
    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private final RefBookVersionRepository versionRepository;
    private final RefBookConflictRepository conflictRepository;

    private final DraftDataService draftDataService;
    private final DropDataService dropDataService;
    private final SearchDataService searchDataService;

    private final RefBookLockService refBookLockService;
    private final VersionService versionService;

    private final VersionValidation versionValidation;

    private final PassportValueRepository passportValueRepository;
    private final AttributeValidationRepository attributeValidationRepository;

    private final VersionFileService versionFileService;

    private final AuditLogService auditLogService;

    private final StrategyLocator strategyLocator;

    private int errorCountLimit = 100;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public DraftServiceImpl(RefBookVersionRepository versionRepository,
                            RefBookConflictRepository conflictRepository,
                            DraftDataService draftDataService, DropDataService dropDataService,
                            SearchDataService searchDataService,
                            RefBookLockService refBookLockService, VersionService versionService,
                            VersionValidation versionValidation,
                            PassportValueRepository passportValueRepository,
                            AttributeValidationRepository attributeValidationRepository,
                            VersionFileService versionFileService,
                            AuditLogService auditLogService,
                            StrategyLocator strategyLocator) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
        this.searchDataService = searchDataService;

        this.refBookLockService = refBookLockService;
        this.versionService = versionService;

        this.versionValidation = versionValidation;

        this.passportValueRepository = passportValueRepository;
        this.attributeValidationRepository = attributeValidationRepository;

        this.versionFileService = versionFileService;
        
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

        return switch (FileUtil.getExtension(fileModel.getName())) {
            case "XLSX" -> createFromXlsx(refBookId, fileModel);
            case "XML" -> createFromXml(refBookId, fileModel);
            default -> throw new FileExtensionException();
        };
    }

    private Draft createFromXlsx(Integer refBookId, FileModel fileModel) {

        RefBookVersionEntityKit kit = findEntityKit(refBookId);
        RefBookEntity refBookEntity = kit.getRefBook();

        Function<Structure, String> newDraftStorage = getNewDraftStorage(refBookEntity);
        BiConsumer<String, Structure> saveDraftConsumer = getSaveDraftConsumer(refBookId);
        RowsProcessor rowsProcessor = new CreateDraftBufferedRowsPersister(draftDataService, newDraftStorage, saveDraftConsumer);

        versionFileService.processRows(fileModel, rowsProcessor, new PlainRowMapper());

        RefBookVersionEntity createdEntity = getStrategy(refBookEntity, FindDraftEntityStrategy.class)
                .find(refBookEntity);
        getStrategy(createdEntity, AfterUploadDataStrategy.class).apply(createdEntity);

        return createdEntity.toDraft();
    }

    private Draft createFromXml(Integer refBookId, FileModel fileModel) {

        Supplier<InputStream> fileSource = versionFileService.supply(fileModel.getPath());

        try (XmlUpdateDraftFileProcessor xmlUpdateDraftFileProcessor = new XmlUpdateDraftFileProcessor(refBookId, this)) {

            Draft draft = xmlUpdateDraftFileProcessor.process(fileSource);
            RefBookVersionEntity createdEntity = versionRepository.getOne(draft.getId());
            uploadDataFromFile(createdEntity, fileModel);
            getStrategy(createdEntity, AfterUploadDataStrategy.class).apply(createdEntity);

            return draft;
        }
    }

    /** Обновление данных черновика из файла. */
    private void uploadDataFromFile(RefBookVersionEntity draftEntity, FileModel fileModel) {

        Structure structure = draftEntity.getStructure();

        validateRows(fileModel, structure, draftEntity.getStorageCode(), // Без учёта локализации
                attributeValidationRepository.findAllByVersionId(draftEntity.getId()));

        persistRows(fileModel, structure, draftEntity.getStorageCode()); // Без учёта локализации
    }

    /** Валидация записей из файла перед загрузкой в БД. */
    private void validateRows(FileModel fileModel, Structure structure, String storageCode,
                              List<AttributeValidationEntity> attributeValidations) {

        RowsProcessor rowsValidator = new RowsValidatorImpl(
                versionService, searchDataService, structure, storageCode,
                errorCountLimit, false, attributeValidations
        );
        StructureRowMapper nonStrictOnTypeRowMapper = new NonStrictOnTypeRowMapper(structure, versionRepository);
        versionFileService.processRows(fileModel, rowsValidator, nonStrictOnTypeRowMapper);
    }

    /** Загрузка записи из файла в БД. */
    private void persistRows(FileModel fileModel, Structure structure, String storageCode) {

        RowsProcessor rowsPersister = new BufferedRowsPersister(draftDataService, storageCode, structure);
        StructureRowMapper structureRowMapper = new StructureRowMapper(structure, versionRepository);
        versionFileService.processRows(fileModel, rowsPersister, structureRowMapper);
    }

    /**
     * Создание хранилища для черновика при загрузке из XLSX (в виде function).
     *
     * @param entity сущность-справочник
     * @return function
     */
    private Function<Structure, String> getNewDraftStorage(RefBookEntity entity) {

        return structure -> createDraftStorage(entity, structure);
    }

    /**
     * Создание черновика при загрузке из XLSX (в виде consumer).
     * <p>
     * Таблица черновика не меняется,
     * т.к. уже изменена внутри CreateDraftBufferedRowsPersister.append .
     *
     * @param refBookId идентификатор справочника
     * @return consumer
     */
    private BiConsumer<String, Structure> getSaveDraftConsumer(Integer refBookId) {

        return (storageCode, structure) -> {

            CreateDraftRequest request = new CreateDraftRequest(refBookId, structure);
            request.setPassport(null);
            request.setReferrerValidationRequired(false);

            create(request, storageCode);
        };
    }

    /** Создание черновика по запросу при наличии уже созданного хранилища. */
    private void create(CreateDraftRequest request, String storageCode) {

        final Integer refBookId = request.getRefBookId();
        versionValidation.validateRefBook(refBookId);

        RefBookVersionEntityKit kit = findEntityKit(refBookId);
        RefBookEntity refBookEntity = kit.getRefBook();

        List<PassportValueEntity> passportValues = (request.getPassport() != null)
                ? RefBookVersionEntity.toPassportValues(request.getPassport(), true, null)
                : null;

        final String refBookCode = refBookEntity.getCode();
        final Structure structure = request.getStructure();

        versionValidation.validateDraftStructure(refBookCode, structure);
        if (request.getReferrerValidationRequired())
            versionValidation.validateReferrerStructure(structure);

        RefBookVersionEntity draftEntity = kit.getDraftEntity();
        if (draftEntity == null) {

            if (passportValues == null) passportValues = kit.getPublishedEntity().getPassportValues();
            draftEntity = createDraftEntity(refBookEntity, structure, passportValues);

        } else {
            draftEntity = recreateDraft(draftEntity, structure, passportValues);
        }

        draftEntity.setStorageCode(storageCode);

        RefBookVersionEntity savedDraftEntity = versionRepository.saveAndFlush(draftEntity);
        addValidations(request.getValidations(), savedDraftEntity);
    }

    /** Создание черновика по запросу при отсутствии уже созданного хранилища. */
    @Override
    @Transactional
    public Draft create(CreateDraftRequest request) {

        final Integer refBookId = request.getRefBookId();
        versionValidation.validateRefBook(refBookId);

        RefBookVersionEntityKit kit = findEntityKit(refBookId);
        RefBookEntity refBookEntity = kit.getRefBook();

        List<PassportValueEntity> passportValues = (request.getPassport() != null)
                ? RefBookVersionEntity.toPassportValues(request.getPassport(), true, null)
                : null;

        final String refBookCode = refBookEntity.getCode();
        final Structure structure = request.getStructure();

        versionValidation.validateDraftStructure(refBookCode, structure);
        if (request.getReferrerValidationRequired())
            versionValidation.validateReferrerStructure(structure);

        RefBookVersionEntity draftEntity = kit.getDraftEntity();
        if (draftEntity == null) {

            if (passportValues == null) passportValues = kit.getPublishedEntity().getPassportValues();
            draftEntity = createDraftEntity(refBookEntity, structure, passportValues);

        } else if (!structure.equals(draftEntity.getStructure())) {

            draftEntity = recreateDraft(draftEntity, structure, passportValues);

        } else {
            deleteDraftAllRows(draftEntity);
            recreatePassportValues(draftEntity, passportValues);
        }

        if (StringUtils.isEmpty(draftEntity.getStorageCode())) {
            String storageCode = createDraftStorage(refBookEntity, draftEntity.getStructure());
            draftEntity.setStorageCode(storageCode);
        }

        RefBookVersionEntity savedDraftEntity = versionRepository.saveAndFlush(draftEntity);
        addValidations(request.getValidations(), savedDraftEntity);

        return savedDraftEntity.toDraft();
    }

    /** Пересоздание черновика при его наличии. */
    private RefBookVersionEntity recreateDraft(RefBookVersionEntity draftEntity, Structure structure,
                                               List<PassportValueEntity> passportValues) {

        RefBookEntity refBookEntity = draftEntity.getRefBook();
        if (passportValues == null) passportValues = draftEntity.getPassportValues();

        removeDraftEntity(draftEntity);
        versionRepository.flush(); // Delete old draft before insert new draft!

        return createDraftEntity(refBookEntity, structure, passportValues);
    }

    /** Пересоздание паспорта черновика при наличии. */
    private void recreatePassportValues(RefBookVersionEntity draftEntity,
                                        List<PassportValueEntity> passportValues) {

        if (passportValues == null) return; // Не менять паспорт, если нет новых значений

        if (!isEmpty(draftEntity.getPassportValues())) {
            passportValueRepository.deleteInBatch(draftEntity.getPassportValues());
        }

        if (!isEmpty(passportValues)) draftEntity.setPassportValues(passportValues);
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

        if (versionEntity.isChangeable())
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

    private RefBookVersionEntity createDraftEntity(RefBookEntity refBookEntity, Structure structure,
                                                   List<PassportValueEntity> passportValues) {

        return getStrategy(refBookEntity, CreateDraftEntityStrategy.class)
                .create(refBookEntity, structure, passportValues);
    }

    private String createDraftStorage(RefBookEntity refBookEntity, Structure structure) {

        return getStrategy(refBookEntity, CreateDraftStorageStrategy.class).create(structure);
    }

    private RefBookVersionEntityKit findEntityKit(Integer refBookId) {

        RefBookVersionEntity publishedEntity = versionRepository
                .findFirstByRefBookIdAndStatusOrderByFromDateDesc(refBookId, RefBookVersionStatus.PUBLISHED);

        RefBookVersionEntity draftEntity = (publishedEntity != null && publishedEntity.isChangeable())
                ? publishedEntity
                : versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookId);

        if (draftEntity == null && publishedEntity == null)
            throw new NotFoundException(new Message(VersionValidationImpl.REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

        return new RefBookVersionEntityKit(publishedEntity, draftEntity);
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
                getStrategy(draftEntity, AddRowValuesStrategy.class).add(draftEntity, addedRowValues);
                addedData = getAddedData(rowValues);
            }

            List<RowValue> updatedRowValues = rowValues.stream().filter(rowValue -> rowValue.getSystemId() != null).collect(toList());
            if (!isEmpty(updatedRowValues)) {
                List<RowValue> currentRowValues = getCurrentRowValues(draftEntity, updatedRowValues);
                getStrategy(draftEntity, UpdateRowValuesStrategy
                        .class).update(draftEntity, currentRowValues, updatedRowValues);
                updatedDiffData = getUpdatedDiffData(currentRowValues, updatedRowValues);
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

    private List<RowValue> getCurrentRowValues(RefBookVersionEntity entity, List<RowValue> updatedRowValues) {

        List<String> fields = entity.getStructure().getAttributeCodes();
        List<Object> systemIds = RowUtils.toSystemIds(updatedRowValues);
        List<RowValue> oldRowValues = searchDataService.findRows(entity.getStorageCode(), fields, systemIds);

        List<Message> messages = systemIds.stream()
                .filter(systemId -> !RowUtils.containsSystemId(oldRowValues, systemId))
                .map(systemId -> new Message(ROW_NOT_FOUND_EXCEPTION_CODE, systemId))
                .collect(toList());
        if (!isEmpty(messages)) throw new UserException(messages);

        return oldRowValues;
    }

    private List<RowDiff> getUpdatedDiffData(List<RowValue> currentRowValues, List<RowValue> updatedRowValues) {

        return currentRowValues.stream()
                .map(oldRowValue -> {
                    RowValue newRowValue = RowUtils.getBySystemId(updatedRowValues, oldRowValue.getSystemId());
                    return RowDiffUtils.getRowDiff(oldRowValue, newRowValue);
                })
                .collect(toList());
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
                getStrategy(draftEntity, DeleteRowValuesStrategy.class).delete(draftEntity, systemIds);

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

        // Исключение полей, не соответствующих атрибутам структуры
        Set<String> attributeCodes = new HashSet<>(draftVersion.getStructure().getAttributeCodes());
        rows.forEach(row -> row.getData().entrySet().removeIf(entry -> !attributeCodes.contains(entry.getKey())));

        if (excludeEmptyRows) {
            rows = rows.stream().filter(row -> !RowUtils.isEmptyRow(row)).collect(toList());
        }
        if (isEmpty(rows)) return emptyList();

        validateDataByType(draftVersion.getStructure(), rows);
        fillSystemIdsByPrimaries(draftVersion, rows);

        return rows;
    }

    /** Валидация добавляемых/обновляемых/удаляемых строк данных по типу. */
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
                .map(row -> RowUtils.toPrimaryKeyValueFilters(row, primaries))
                .filter(list -> !isEmpty(list))
                .collect(toSet());
        criteria.setAttributeFilters(filterSet);

        Page<RefBookRowValue> rowValues = versionService.search(draftVersion.getId(), criteria);
        if (rowValues == null || isEmpty(rowValues.getContent()))
            return;

        rowValues.getContent().forEach(rowValue ->
                rows.stream()
                        .filter(row -> row.getSystemId() == null)
                        .filter(row -> RowUtils.equalsValuesByAttributes(row, rowValue, primaries))
                        .forEach(row -> row.setSystemId(rowValue.getSystemId()))
        );
    }

    /** Валидация добавляемых/обновляемых строк данных по структуре. */
    private void validateDataByStructure(RefBookVersionEntity draftVersion, List<Row> rows) {

        if (isEmpty(rows)) return;

        RowsValidator validator = new RowsValidatorImpl(
                versionService, searchDataService,
                draftVersion.getStructure(), draftVersion.getStorageCode(), errorCountLimit, false,
                attributeValidationRepository.findAllByVersionId(draftVersion.getId())
        );
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
    private void deleteDraftAllRows(RefBookVersionEntity draftEntity) {

        getStrategy(draftEntity, DeleteAllRowValuesStrategy.class).deleteAll(draftEntity);
    }

    @Override
    public void updateFromFile(Integer draftId, UpdateFromFileRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        if (draftEntity.hasEmptyStructure())
            throw new UserException(new Message(VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE, draftEntity.getId()));

        Integer refBookId = draftEntity.getRefBook().getId();
        refBookLockService.setRefBookUpdating(refBookId);
        try {
            uploadDataFromFile(draftEntity, request.getFileModel());
            forceUpdateOptLockValue(versionRepository.findById(draftId).orElse(null));

            getStrategy(draftEntity, AfterUploadDataStrategy.class).apply(draftEntity);

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

        RefBookVersionEntity entity = findChangeableOrThrow(draftId);
        return getRowValuesOfDraft(entity, criteria);
    }

    private Page<RefBookRowValue> getRowValuesOfDraft(RefBookVersionEntity draft, SearchDataCriteria criteria) {

        List<Field> fields = makeOutputFields(draft, criteria.getLocaleCode());

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getAttributeFilters()));
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getPlainAttributeFilters(), draft.getStructure()));

        String storageCode = toStorageCode(draft, criteria);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(storageCode,
                null, null, // Черновик
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

        RefBookVersionEntity entity = findChangeableOrThrow(draftId);
        return searchDataService.hasData(entity.getStorageCode());
    }

    @Override
    @Transactional
    public void remove(Integer draftId) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.validateRefBookNotBusy(draftEntity.getRefBook().getId());
        removeDraftEntity(draftEntity);
    }

    /**
     * Удаление черновика.
     */
    private void removeDraftEntity(RefBookVersionEntity draftEntity) {

        dropDataService.drop(singleton(draftEntity.getStorageCode()));
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftEntity.getId());
        versionRepository.deleteById(draftEntity.getId());
    }

    @Override
    @Transactional
    public Draft getDraft(Integer draftId) {

        return findChangeableOrThrow(draftId).toDraft();
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

        Structure.Attribute attribute = getStrategy(draftEntity, CreateAttributeStrategy.class)
                .create(draftEntity, request);

        forceUpdateOptLockValue(draftEntity);

        auditStructureEdit(draftEntity, "create_attribute", attribute);
    }

    @Override
    @Transactional
    public void updateAttribute(Integer draftId, UpdateAttributeRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.validateRefBookNotBusy(draftEntity.getRefBook().getId());
        validateOptLockValue(draftEntity, request);

        Structure.Attribute attribute = getStrategy(draftEntity, UpdateAttributeStrategy.class)
                .update(draftEntity, request);

        forceUpdateOptLockValue(draftEntity);

        auditStructureEdit(draftEntity, "update_attribute", attribute);
    }

    @Override
    @Transactional
    public void deleteAttribute(Integer draftId, DeleteAttributeRequest request) {

        RefBookVersionEntity draftEntity = findForUpdate(draftId);

        refBookLockService.validateRefBookNotBusy(draftEntity.getRefBook().getId());
        validateOptLockValue(draftEntity, request);

        Structure.Attribute attribute = getStrategy(draftEntity, DeleteAttributeStrategy.class)
                .delete(draftEntity, request);

        forceUpdateOptLockValue(draftEntity);

        attributeValidationRepository.deleteByVersionIdAndAttribute(draftEntity.getId(), attribute.getCode());

        auditStructureEdit(draftEntity, "delete_attribute", attribute);
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

        if (attribute == null) {
            attributeValidationRepository.deleteByVersionId(draftId);

            return;
        }

        versionValidation.validateDraftAttributeExists(draftId, draftEntity.getStructure(), attribute);
        if (type == null) {
            attributeValidationRepository.deleteByVersionIdAndAttribute(draftId, attribute);

        } else {
            attributeValidationRepository.deleteByVersionIdAndAttributeAndType(draftId, attribute, type);
        }
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
        RowsValidator validator = new RowsValidatorImpl(
                versionService, searchDataService,
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

        if (fileType == null) return null;

        RefBookVersionEntity entity = findChangeableOrThrow(draftId);

        RefBookVersion version = ModelGenerator.versionModel(entity);
        ExportFile exportFile = versionFileService.getFile(version, fileType, versionService);

        auditLogService.addAction(AuditAction.DOWNLOAD, () -> entity);

        return exportFile;
    }

    private void validateOptLockValue(RefBookVersionEntity entity, DraftChangeRequest request) {

        versionValidation.validateOptLockValue(entity.getId(), entity.getOptLockValue(), request.getOptLockValue());
    }

    protected RefBookVersionEntity findForUpdate(Integer id) {

        RefBookVersionEntity entity = findChangeableOrThrow(id);
        getStrategy(entity, ValidateVersionNotArchivedStrategy.class).validate(entity);

        return entity;
    }

    private RefBookVersionEntity findChangeableOrThrow(Integer id) {

        RefBookVersionEntity entity = findVersion(id);
        if (entity == null || !entity.isChangeable())
            throw new NotFoundException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, id));

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

        return getStrategy(entity != null ? entity.getRefBook() : null, strategy);
    }

    private <T extends Strategy> T getStrategy(RefBookEntity entity, Class<T> strategy) {

        return strategyLocator.getStrategy(entity != null ? entity.getType() : null, strategy);
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