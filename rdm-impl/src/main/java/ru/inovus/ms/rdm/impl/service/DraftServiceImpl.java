package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.components.common.exception.CodifiedException;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.*;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refdata.*;
import ru.inovus.ms.rdm.api.model.validation.*;
import ru.inovus.ms.rdm.api.model.version.*;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.VersionFileService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.util.FileNameGenerator;
import ru.inovus.ms.rdm.api.util.RowUtils;
import ru.inovus.ms.rdm.api.util.StructureUtils;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.*;
import ru.inovus.ms.rdm.impl.file.*;
import ru.inovus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.inovus.ms.rdm.impl.file.process.*;
import ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates;
import ru.inovus.ms.rdm.impl.repository.*;
import ru.inovus.ms.rdm.impl.util.*;
import ru.inovus.ms.rdm.impl.util.mappers.*;
import ru.inovus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.inovus.ms.rdm.impl.validation.TypeValidation;
import ru.inovus.ms.rdm.impl.validation.VersionValidationImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@Primary
@Service
public class DraftServiceImpl implements DraftService {

    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";

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
        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        switch (extension) {
            case "XLSX": return updateDraftDataByXlsx(refBookId, fileModel, inputStreamSupplier);
            case "XML": return updateDraftDataByXml(refBookId, fileModel, inputStreamSupplier);
            default: throw new UserException("file.extension.invalid");
        }
    }

    private Draft updateDraftDataByXlsx(Integer refBookId, FileModel fileModel, Supplier<InputStream> inputStreamSupplier) {

        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        BiConsumer<String, Structure> saveDraftConsumer = getSaveDraftConsumer(refBookId);
        RowsProcessor rowsProcessor = new CreateDraftBufferedRowsPersister(draftDataService, saveDraftConsumer);
        processFileRows(extension, rowsProcessor, new PlainRowMapper(), inputStreamSupplier);

        RefBookVersionEntity createdDraft = getDraftByRefBook(refBookId);
        return new Draft(createdDraft.getId(), createdDraft.getStorageCode());
    }

    private Draft updateDraftDataByXml(Integer refBookId, FileModel fileModel, Supplier<InputStream> inputStreamSupplier) {

        try (XmlUpdateDraftFileProcessor xmlUpdateDraftFileProcessor = new XmlUpdateDraftFileProcessor(refBookId, this)) {
            Draft draft = xmlUpdateDraftFileProcessor.process(inputStreamSupplier);
            updateDraftData(versionRepository.getOne(draft.getId()), fileModel);
            return draft;
        }
    }

    /** Обновление данных черновика из файла. */
    private void updateDraftData(RefBookVersionEntity draft, FileModel fileModel) {

        Structure structure = draft.getStructure();

        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
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
    private void processFileRows(String extension, RowsProcessor rowsProcessor, RowMapper rowMapper, Supplier<InputStream> fileSupplier) {

        try (FilePerRowProcessor persister = FileProcessorFactory.createProcessor(extension, rowsProcessor, rowMapper)) {
            persister.process(fileSupplier);

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    private BiConsumer<String, Structure> getSaveDraftConsumer(Integer refBookId) {
        return (storageCode, structure) -> {
            versionValidation.validateStructure(structure);

            RefBookVersionEntity lastRefBookVersion = getLastRefBookVersion(refBookId);
            RefBookVersionEntity draftVersion = getDraftByRefBook(refBookId);
            if (draftVersion == null && lastRefBookVersion == null)
                throw new NotFoundException(new Message(VersionValidationImpl.REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

            // NB: structure == null means that draft was created during passport saving
            if (draftVersion != null && draftVersion.getStructure() != null) {
                dropDataService.drop(singleton(draftVersion.getStorageCode()));
                versionRepository.deleteById(draftVersion.getId());
                versionRepository.flush(); // Delete old draft before insert new draft!

                draftVersion = newDraftVersion(structure, draftVersion.getPassportValues());

            } else if (draftVersion == null) {
                draftVersion = newDraftVersion(structure, lastRefBookVersion.getPassportValues());

            } else {
                draftVersion.setStructure(structure);
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
            throw new CodifiedException("invalid refbook");

        List<PassportValueEntity> passportValues = null;
        if (createDraftRequest.getPassport() != null) {
            passportValues = createDraftRequest.getPassport().entrySet().stream()
                    .map(entry -> new PassportValueEntity(new PassportAttributeEntity(entry.getKey()), (String) entry.getValue(), null))
                    .collect(toList());
        }

        final Structure structure = createDraftRequest.getStructure();
        versionValidation.validateStructure(structure);

        List<Field> fields = ConverterUtil.fields(structure);
        if (draftVersion == null) {
            draftVersion = newDraftVersion(structure, passportValues != null ? passportValues : lastRefBookVersion.getPassportValues());
            draftVersion.setRefBook(newRefBook(refBookId));
            String draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);
            draftVersion.getRefBook().setCode(lastRefBookVersion.getRefBook().getCode());
        } else {
            draftVersion = updateDraft(structure, draftVersion, fields, passportValues);
        }

        RefBookVersionEntity savedDraftVersion = versionRepository.save(draftVersion);
        addValidations(createDraftRequest.getFieldValidations(), savedDraftVersion);
        return new Draft(savedDraftVersion.getId(), savedDraftVersion.getStorageCode());
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
        CreateDraftRequest draftRequest  = new CreateDraftRequest(sourceVersion.getRefBook().getId(), sourceVersion.getStructure(), passport, validations);
        Draft draft = create(draftRequest);

        draftDataService.loadData(draft.getStorageCode(), sourceVersion.getStorageCode(), sourceVersion.getFromDate(), sourceVersion.getToDate());
        conflictRepository.copyByReferrerVersion(versionId, draft.getId());

        return draft;
    }

    private RefBookVersionEntity updateDraft(Structure structure, RefBookVersionEntity draftVersion, List<Field> fields, List<PassportValueEntity> passportValues) {

        versionValidation.validateStructure(structure);

        String draftCode = draftVersion.getStorageCode();

        if (!structure.equals(draftVersion.getStructure())) {
            Integer refBookId = draftVersion.getRefBook().getId();

            if (passportValues == null) passportValues = draftVersion.getPassportValues();

            dropDataService.drop(singleton(draftCode));
            versionRepository.deleteById(draftVersion.getId());

            draftVersion = newDraftVersion(structure, passportValues);
            draftVersion.setRefBook(newRefBook(refBookId));
            draftCode = draftDataService.createDraft(fields);
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

        versionValidation.validateStructure(structure);

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

    private void setSystemIdIfPossible(Structure structure, List<Row> sourceRows, int draftId) {

        List<Structure.Attribute> primaryKeys = structure.getPrimary();
        if (primaryKeys.isEmpty()) return;

        List<AttributeFilter> filters = sourceRows.stream()
                .filter(row -> row.getSystemId() == null)
                .map(row -> RowUtils.getPrimaryKeyValueFilters(row, primaryKeys))
                .flatMap(Collection::stream).collect(toList());
        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(sourceRows.size());
        criteria.setAttributeFilter(Set.of(filters));
        Page<RefBookRowValue> search = versionService.search(draftId, criteria);
        for (RefBookRowValue refBookRowValue : search.getContent()) {
            for (Row row : sourceRows) {
                if (row.getSystemId() == null && RowUtils.equalsValuesByAttributes(row, refBookRowValue, primaryKeys)) {
                    row.setSystemId(refBookRowValue.getSystemId());
                }
            }
        }
    }

    private List<Row> preprocessRows(List<Row> rows, RefBookVersionEntity draftVersion, boolean removeEvenIfSystemIdIsPresent) {
        if (isEmpty(rows)) return emptyList();

        Set<String> attributeCodes = StructureUtils.getAttributeCodes(draftVersion.getStructure()).collect(toSet());

        Stream<Row> stream = rows.stream().peek(row ->
                row.getData().entrySet().removeIf(entry -> !attributeCodes.contains(entry.getKey()))
        );
        if (removeEvenIfSystemIdIsPresent)
            stream = stream.filter(row -> !RowUtils.isEmptyRow(row));

        rows = stream.collect(toList());
        if (isEmpty(rows)) return emptyList();

        validateTypeSafety(rows, draftVersion.getStructure());
        setSystemIdIfPossible(draftVersion.getStructure(), rows, draftVersion.getId());

        return rows;
    }

    @Override
    @Transactional
    public void updateData(Integer draftId, Row row) {
        updateData(draftId, singletonList(row));
    }

    @Override
    @Transactional
    public void updateData(Integer draftId, List<Row> rows) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);
        refBookLockService.setRefBookUpdating(draftVersion.getRefBook().getId());
        List<Object> addedData = null;
        List<RowDiff> rowDiffs = null;
        try {
            rows = preprocessRows(rows, draftVersion, true);
            if (rows.isEmpty()) return;
            List<RowValue> convertedRows = rows.stream().map(row -> ConverterUtil.rowValue((row), draftVersion.getStructure())).collect(toList());
            validateDataByStructure(draftVersion, rows);

            List<RowValue> addedRowValues = convertedRows.stream().filter(rowValue -> rowValue.getSystemId() == null).collect(toList());
            if (!isEmpty(addedRowValues)) {
                try {
                    draftDataService.addRows(draftVersion.getStorageCode(), addedRowValues);
                } catch (RuntimeException e) {
                    ErrorUtil.rethrowError(e);
                }
                addedData = addedRowValues.stream().map(RowValue::getFieldValues).collect(toList());
            }

            List<RowValue> updatedRowValues = convertedRows.stream().filter(rowValue -> rowValue.getSystemId() != null).collect(toList());
            if (!isEmpty(updatedRowValues)) {
                List<String> fields = StructureUtils.getAttributeCodes(draftVersion.getStructure()).collect(toList());
                List<Object> systemIds = updatedRowValues.stream().map(RowValue::getSystemId).collect(toList());
                List<RowValue> oldRowValues = searchDataService.findRows(draftVersion.getStorageCode(), fields, systemIds);

                List<Message> messages = systemIds.stream()
                        .filter(systemId -> !RowUtils.isSystemIdRowValue(systemId, oldRowValues))
                        .map(systemId -> new Message(ROW_NOT_FOUND_EXCEPTION_CODE, systemId))
                        .collect(toList());
                if (!isEmpty(messages)) throw new UserException(messages);

                rowDiffs = oldRowValues.stream()
                        .map(oldRowValue -> {
                            RowValue newRowValue = RowUtils.getSystemIdRowValue(oldRowValue.getSystemId(), updatedRowValues);
                            return RowDiffUtils.getRowDiff(oldRowValue, newRowValue);
                        })
                        .collect(toList());

                conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(draftVersion.getId(), RowUtils.toLongSystemIds(systemIds));
                try {
                    draftDataService.updateRows(draftVersion.getStorageCode(), updatedRowValues);
                } catch (RuntimeException e) {
                    ErrorUtil.rethrowError(e);
                }
            }
        } finally {
            refBookLockService.deleteRefBookOperation(draftVersion.getRefBook().getId());
        }
        auditEditData(draftVersion, Map.of("create_rows", addedData == null ? "-" : addedData, "update_rows", rowDiffs == null ? "-" : rowDiffs));
    }

    private void validateTypeSafety(List<Row> rows, Structure structure) {
        NonStrictOnTypeRowMapper mapper = new NonStrictOnTypeRowMapper(structure, versionRepository);
        List<Message> errors = rows.stream().map(mapper::map).map(row -> new TypeValidation(row.getData(), structure).validate()).flatMap(Collection::stream).collect(toList());
        if (!errors.isEmpty())
            throw new UserException(errors);
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
    public void deleteRow(Integer draftId, Row row) {
        deleteRows(draftId, singletonList(row));
    }

    @Override
    @Transactional
    public void deleteRows(Integer draftId, List<Row> rows) {
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);
        versionValidation.validateDraft(draftId);
        rows = preprocessRows(rows, draftVersion, false);
        List<Object> systemIds;
        refBookLockService.setRefBookUpdating(draftVersion.getRefBook().getId());
        try {
            systemIds = rows.stream().filter(row -> row.getSystemId() != null).map(Row::getSystemId).collect(toList());
            if (!systemIds.isEmpty()) {
                conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(draftVersion.getId(), RowUtils.toLongSystemIds(systemIds));
                draftDataService.deleteRows(draftVersion.getStorageCode(), systemIds);
            }
        } finally {
            refBookLockService.deleteRefBookOperation(draftVersion.getRefBook().getId());
        }
        auditEditData(draftVersion, "delete_rows", systemIds);
    }

    @Override
    @Transactional
    public void deleteAllRows(Integer draftId) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);
        refBookLockService.setRefBookUpdating(draftVersion.getRefBook().getId());
        try {
            deleteDraftAllRows(draftVersion);

        } finally {
            refBookLockService.deleteRefBookOperation(draftVersion.getRefBook().getId());
        }

        auditEditData(draftVersion, "delete_all_rows", "-");
    }

    private void deleteDraftAllRows(RefBookVersionEntity draftVersion) {

        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftVersion.getId());
        draftDataService.deleteAllRows(draftVersion.getStorageCode());
    }

    @Override
    public void updateData(Integer draftId, FileModel fileModel) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.findById(draftId).orElseThrow();

        Integer refBookId = draftVersion.getRefBook().getId();
        refBookLockService.setRefBookUpdating(refBookId);
        try {
            updateDraftData(draftVersion, fileModel);

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        // Нельзя просто передать draftVersion, так как в аудите подтягиваются значения паспорта справочника
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

    private RefBookVersionEntity getLastRefBookVersion(Integer refBookId) {
        Page<RefBookVersionEntity> lastPublishedVersions = versionRepository
                .findAll(RefBookVersionPredicates.isPublished().and(RefBookVersionPredicates.isVersionOfRefBook(refBookId)),
                        PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "fromDate")));
        return lastPublishedVersions.hasContent() ? lastPublishedVersions.getContent().get(0) : null;
    }

    @Override
    @Transactional
    public void remove(Integer draftId) {

        versionValidation.validateDraft(draftId);
        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        versionRepository.deleteById(draftId);
    }

    @Override
    @Transactional
    public Draft getDraft(Integer draftId) {

        versionValidation.validateDraftExists(draftId);
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);
        return new Draft(draftVersion.getId(), draftVersion.getStorageCode());
    }

    @Override
    @Transactional
    public void createAttribute(CreateAttribute createAttribute) {

        versionValidation.validateDraft(createAttribute.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(createAttribute.getVersionId());

        RefBookVersionEntity draftEntity = versionRepository.getOne(createAttribute.getVersionId());
        Structure structure = draftEntity.getStructure();
        if (structure == null)
            structure = new Structure();

        structureChangeValidator.validateCreateAttribute(createAttribute);

        Structure.Attribute attribute = createAttribute.getAttribute();
        validateAttribute(attribute, structure, draftEntity.getRefBook().getCode());

        validateRequired(attribute, draftEntity.getStorageCode(), structure);

        // Clear previous primary keys:
        if (attribute.hasIsPrimary())
            structure.clearPrimary();

        Structure.Reference reference = createAttribute.getReference();
        if (reference != null && reference.isNull())
            reference = null;

        if (reference != null) {
            validateReference(attribute, reference, structure, draftEntity.getRefBook().getCode());
        }

        try {
            draftDataService.addField(draftEntity.getStorageCode(), ConverterUtil.field(attribute));

        } catch (CodifiedException ce) {
            throw new UserException(new Message(ce.getMessage(), ce.getArgs()), ce);
        }

        structure.add(attribute, reference);
        draftEntity.setStructure(structure);

        auditStructureEdit(draftEntity, "create_attribute", attribute);
    }

    /** Проверка наличия данных для добавляемого атрибута, обязательного к заполнению. */
    private void validateRequired(Structure.Attribute attribute, String storageCode, Structure structure) {

        if (structure == null || structure.getAttributes() == null || !attribute.hasIsPrimary())
            return;

        DataCriteria dataCriteria = new DataCriteria(storageCode, null, null, ConverterUtil.fields(structure), emptySet(), null);
        dataCriteria.setCount(1);
        dataCriteria.setPage(DataCriteria.MIN_PAGE);
        dataCriteria.setSize(DataCriteria.MIN_SIZE);

        Collection<RowValue> data = searchDataService.getPagedData(dataCriteria).getCollection();
        if (!isEmpty(data))
            throw new UserException(new Message("validation.required.err", attribute.getName()));
    }

    @Override
    @Transactional
    public void updateAttribute(UpdateAttribute updateAttribute) {

        versionValidation.validateDraft(updateAttribute.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(updateAttribute.getVersionId());

        RefBookVersionEntity draftEntity = versionRepository.getOne(updateAttribute.getVersionId());
        Structure structure = draftEntity.getStructure();

        Structure.Attribute oldAttribute = structure.getAttribute(updateAttribute.getCode());
        structureChangeValidator.validateUpdateAttribute(updateAttribute, oldAttribute);

        Structure.Attribute newAttribute = Structure.Attribute.build(oldAttribute);
        updateAttribute.fillAttribute(newAttribute);
        validateAttribute(newAttribute, structure, draftEntity.getRefBook().getCode());

        // Clear previous primary keys:
        if (newAttribute.hasIsPrimary())
            structure.clearPrimary();

        Structure.Reference oldReference = structure.getReference(oldAttribute.getCode());
        Structure.Reference newReference = null;
        if (newAttribute.isReferenceType()) {
            newReference = Structure.Reference.build(oldReference);
            updateAttribute.fillReference(newReference);
            validateReference(newAttribute, newReference, structure, draftEntity.getRefBook().getCode());
        }

        structureChangeValidator.validateUpdateAttributeStorage(updateAttribute, oldAttribute, draftEntity.getStorageCode());

        try {
            draftDataService.updateField(draftEntity.getStorageCode(), ConverterUtil.field(newAttribute));

        } catch (CodifiedException ce) {
            throw new UserException(new Message(ce.getMessage(), ce.getArgs()), ce);
        }

        structure.update(oldAttribute, newAttribute);
        structure.update(oldReference, newReference);

        // Обновление значений ссылки только по необходимости:
        if (!StructureUtils.isDisplayExpressionEquals(oldReference, newReference)) {
            refreshReferenceDisplayValues(draftEntity, newReference);
        }

        if (Objects.equals(oldAttribute.getType(), updateAttribute.getType())) {
            attributeValidationRepository.deleteAll(
                    attributeValidationRepository.findAllByVersionIdAndAttribute(updateAttribute.getVersionId(), updateAttribute.getCode()));
        }

        auditStructureEdit(draftEntity, "update_attribute", newAttribute);
    }

    private void validateAttribute(Structure.Attribute newAttribute, Structure oldStructure, String refBookCode) {

        if (!newAttribute.hasIsPrimary() && !newAttribute.isReferenceType()
                && !isEmpty(oldStructure.getReferences()) && !oldStructure.hasPrimary())
            throw new UserException(new Message(VersionValidationImpl.REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));

        versionValidation.validateAttribute(newAttribute);
    }

    private void validateReference(Structure.Attribute newAttribute, Structure.Reference newReference,
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
    public void deleteAttribute(Integer draftId, String attributeCode) {

        versionValidation.validateDraft(draftId);
        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);
        Structure structure = draftEntity.getStructure();

        Structure.Attribute attribute = structure.getAttribute(attributeCode);
        structure.remove(attributeCode);
        versionValidation.validateStructure(structure);

        try {
            draftDataService.deleteField(draftEntity.getStorageCode(), attributeCode);

        } catch (RuntimeException e) {
            ErrorUtil.rethrowError(e);
        }
        attributeValidationRepository.deleteAll(attributeValidationRepository.findAllByVersionIdAndAttribute(draftId, attributeCode));

        auditStructureEdit(draftEntity, "delete_attribute", attribute);
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

    @Override
    public Integer getIdByRefBookCode(String refBookCode) {
        RefBookVersionEntity draftEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.DRAFT);
        if (draftEntity == null)
            return null;
        return draftEntity.getId();
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