package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.apache.commons.io.FilenameUtils;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.i_novus.components.common.exception.CodifiedException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;
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
import ru.inovus.ms.rdm.impl.validation.AttributeUpdateValidator;
import ru.inovus.ms.rdm.impl.validation.TypeValidation;
import ru.inovus.ms.rdm.impl.validation.VersionValidationImpl;

import javax.persistence.PersistenceException;
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
    private static final String ROW_NOT_UNIQUE = "row.not.unique";

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
    private AttributeUpdateValidator attributeUpdateValidator;

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
                            AttributeValidationRepository attributeValidationRepository, AttributeUpdateValidator attributeUpdateValidator,
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
        this.attributeUpdateValidator = attributeUpdateValidator;

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
    private void processFileRows(String extension, RowsProcessor rowsProcessor, RowMapper rowMapper,
                                 Supplier<InputStream> fileSupplier) {
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
        if (primaryKeys.isEmpty())
            return;

        List<AttributeFilter> filters = sourceRows.stream()
                .filter(row -> row.getSystemId() == null)
                .map(row -> RowUtils.getPrimaryKeyValueFilters(row, primaryKeys))
                .flatMap(Collection::stream).collect(toList());
        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setAttributeFilter(Set.of(filters));

        int page = RestCriteria.FIRST_PAGE_NUMBER;
        criteria.setPageNumber(page);

        Page<RefBookRowValue> search;
        while (page < (search = versionService.search(draftId, criteria)).getTotalPages()) {
            criteria.setPageNumber(++page);
            for (RefBookRowValue oldValue : search) {
                for (Row row : sourceRows) {
                    if (row.getSystemId() == null
                            && RowUtils.equalsValuesByAttributes(row, oldValue, primaryKeys))
                        row.setSystemId(oldValue.getSystemId());
                }
            }
        }
    }

    private List<Row> preprocessRows(List<Row> rows, RefBookVersionEntity draftVersion, boolean removeEvenIfSystemIdIsPresent) {
        if (isEmpty(rows))
            return emptyList();

        Set<String> attributeCodes = StructureUtils.getAttributeCodes(draftVersion.getStructure()).collect(toSet());
        Stream<Row> stream = rows.stream()
                .peek(row -> row.getData().entrySet().removeIf(entry -> !attributeCodes.contains(entry.getKey())));
        if (removeEvenIfSystemIdIsPresent)
            stream = stream.filter(row -> !RowUtils.isEmptyRow(row));

        rows = stream.collect(toList());
        if (isEmpty(rows))
            return emptyList();

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
        try {
            rows = preprocessRows(rows, draftVersion, true);
            if (rows.isEmpty()) return;
            List<RowValue> convertedRows = rows.stream().map(row -> ConverterUtil.rowValue((row), draftVersion.getStructure())).collect(toList());
            validateDataByStructure(draftVersion, rows);

            List<RowValue> addedRowValues = convertedRows.stream().filter(rowValue -> rowValue.getSystemId() == null).collect(toList());
            if (!isEmpty(addedRowValues)) {
                mutateCatchNotUniqueAndRethrow(() -> draftDataService.addRows(draftVersion.getStorageCode(), addedRowValues));

                List<Object> addedData = addedRowValues.stream().map(RowValue::getFieldValues).collect(toList());
                auditEditData(draftVersion, "create_rows", addedData);
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

                List<RowDiff> rowDiffs = oldRowValues.stream()
                        .map(oldRowValue -> {
                            RowValue newRowValue = RowUtils.getSystemIdRowValue(oldRowValue.getSystemId(), updatedRowValues);
                            return RowDiffUtils.getRowDiff(oldRowValue, newRowValue);
                        })
                        .collect(toList());

                conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(draftVersion.getId(), RowUtils.toLongSystemIds(systemIds));
                mutateCatchNotUniqueAndRethrow(() -> draftDataService.updateRows(draftVersion.getStorageCode(), updatedRowValues));

                auditEditData(draftVersion, "update_rows", rowDiffs);
            }
        } finally {
            refBookLockService.deleteRefBookOperation(draftVersion.getRefBook().getId());
        }
    }

    private void mutateCatchNotUniqueAndRethrow(Runnable exec) {
        try {
            exec.run();
        } catch (NotUniqueException e) {
            throw new UserException(ROW_NOT_UNIQUE, e);
        } catch (PersistenceException e) {
            boolean notUnique = false;
            if (e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause() instanceof PSQLException)
                notUnique = "23505".equals(((PSQLException) e.getCause().getCause()).getSQLState());
            if (notUnique)
                throw new UserException(ROW_NOT_UNIQUE, e);
            else throw e;
        }
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
        refBookLockService.setRefBookUpdating(draftVersion.getRefBook().getId());
        try {
            List<Object> systemIds = rows.stream().filter(row -> row.getSystemId() != null).map(Row::getSystemId).collect(toList());
            if (!systemIds.isEmpty()) {
                conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(draftVersion.getId(), RowUtils.toLongSystemIds(systemIds));
                draftDataService.deleteRows(draftVersion.getStorageCode(), systemIds);

                auditEditData(draftVersion, "delete_row", systemIds);
            }
        } finally {
            refBookLockService.deleteRefBookOperation(draftVersion.getRefBook().getId());
        }
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
//      Нельзя просто передать draftVersion, так как в аудите подтягиваются значения пасспорта справочника (а у них lazy инициализация), поэтому нужна транзакция (которой в этом методе нет)
        auditLogService.addAction(AuditAction.UPLOAD_DATA, () -> versionRepository.findById(draftId).get());
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

        Structure.Attribute attribute = createAttribute.getAttribute();
        validateRequired(attribute, draftEntity.getStorageCode(), structure);
        versionValidation.validateStructure(structure);

        //clear previous primary keys
        if (createAttribute.getAttribute().getIsPrimary())
            structure.clearPrimary();

        Structure.Reference reference = createAttribute.getReference();
        boolean isReference = Objects.nonNull(reference) && !reference.isNull();
        if (isReference != attribute.isReferenceType()) throw new IllegalArgumentException("Can not update structure, illegal create attribute");

        if (isReference) {
            validateRef(reference.getDisplayExpression(), reference.getReferenceCode());
        }

        draftDataService.addField(draftEntity.getStorageCode(), ConverterUtil.field(attribute));

        if (structure == null) {
            structure = new Structure();
        }
        if (structure.getAttributes() == null)
            structure.setAttributes(new ArrayList<>());

        structure.getAttributes().add(attribute);

        if (isReference) {
            if (structure.getReferences() == null)
                structure.setReferences(new ArrayList<>());
            structure.getReferences().add(reference);
        }
        draftEntity.setStructure(structure);

        auditStructureEdit(draftEntity, "create_attribute", createAttribute.getAttribute());
    }

    private void validateRequired(Structure.Attribute attribute, String storageCode, Structure structure) {
        if (structure != null && structure.getAttributes() != null && attribute.getIsPrimary()) {
            List<RowValue> data = searchDataService.getData(
                    new DataCriteria(storageCode, null, null, ConverterUtil.fields(structure), emptySet(), null)
            );
            if (!isEmpty(data)) {
                throw new UserException(new Message("validation.required.err", attribute.getName()));
            }
        }
    }

    @Override
    @Transactional
    public void updateAttribute(UpdateAttribute updateAttribute) {

        versionValidation.validateDraft(updateAttribute.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(updateAttribute.getVersionId());

        RefBookVersionEntity draftEntity = versionRepository.getOne(updateAttribute.getVersionId());
        Structure structure = draftEntity.getStructure();

        Structure.Attribute attribute = structure.getAttribute(updateAttribute.getCode());
        attributeUpdateValidator.validateUpdateAttribute(updateAttribute, attribute, draftEntity.getStorageCode());
        versionValidation.validateStructure(structure);

        if (updateAttribute.isReferenceType()) {
            String newDisplayExpression = updateAttribute.getDisplayExpression().get();
            Structure.Reference reference = structure.getReference(updateAttribute.getCode());
            String oldDisplayExpression = Objects.nonNull(reference) ? reference.getDisplayExpression() : null;

            if (Objects.isNull(oldDisplayExpression)
                    || !oldDisplayExpression.equals(newDisplayExpression)) {
                validateRef(newDisplayExpression, updateAttribute.getReferenceCode().get());
            }
        }

        //clear previous primary keys
        if (updateAttribute.getIsPrimary() != null
                && updateAttribute.getIsPrimary().isPresent()
                && updateAttribute.getIsPrimary().get())
            structure.clearPrimary();

        FieldType oldType = attribute.getType();
        fillUpdatableAttribute(updateAttribute, attribute);

        try {
            draftDataService.updateField(draftEntity.getStorageCode(), ConverterUtil.field(attribute));

        } catch (CodifiedException ce) {
            throw new UserException(ce.getMessage(), ce);
        }

        fillUpdatableReference(updateAttribute, draftEntity, structure, oldType);

        if (Objects.equals(oldType, updateAttribute.getType())) {
            attributeValidationRepository.deleteAll(
                    attributeValidationRepository.findAllByVersionIdAndAttribute(updateAttribute.getVersionId(), updateAttribute.getCode()));
        }

        auditStructureEdit(draftEntity, "update_attribute", structure.getAttribute(updateAttribute.getCode()));
    }

    private void validateRef(String displayExpression, String refBookCode) {

        if (StringUtils.isEmpty(displayExpression))
            return; // NB: to-do: throw exception and fix absent referredBook in testLifecycle.

        RefBookVersion referredVersion = versionService.getLastPublishedVersion(refBookCode);
        versionValidation.validateReferenceDisplayExpression(displayExpression, referredVersion);
        List<Structure.Attribute> pks = referredVersion.getStructure().getPrimary();
        if (pks.size() != 1) throw new UserException("referenced.refbook.has.more.or.less.than.one.pk");
    }

    private void fillUpdatableAttribute(UpdateAttribute updateAttribute, Structure.Attribute attribute) {
        setValueIfPresent(updateAttribute::getName, attribute::setName);
        setValueIfPresent(updateAttribute::getDescription, attribute::setDescription);
        setValueIfPresent(updateAttribute::getIsPrimary, attribute::setPrimary);
        attribute.setType(updateAttribute.getType());
    }

    private void fillUpdatableReference(UpdateAttribute updateAttribute, RefBookVersionEntity draftEntity,
                                        Structure structure, FieldType oldType) {
        if (updateAttribute.isReferenceType()) {
            Structure.Reference reference = (FieldType.REFERENCE.equals(oldType))
                    ? structure.getReference(updateAttribute.getCode())
                    : new Structure.Reference();

            String oldDisplayExpression = reference.getDisplayExpression();

            int updatableReferenceIndex = structure.getReferences().indexOf(reference);
            fillUpdatableReference(updateAttribute, reference);
            if (updatableReferenceIndex >= 0)
                structure.getReferences().set(updatableReferenceIndex, reference);
            else
                structure.getReferences().add(reference);

            if (Objects.isNull(oldDisplayExpression)
                    || !oldDisplayExpression.equals(updateAttribute.getDisplayExpression().get())) {
                refreshReferenceDisplayValues(draftEntity, reference);
            }

        } else if (FieldType.REFERENCE.equals(oldType)) {
            structure.getReferences().remove(structure.getReference(updateAttribute.getCode()));
        }
    }

    private void fillUpdatableReference(UpdateAttribute updateAttribute, Structure.Reference reference) {
        setValueIfPresent(updateAttribute::getAttribute, reference::setAttribute);
        setValueIfPresent(updateAttribute::getReferenceCode, reference::setReferenceCode);
        setValueIfPresent(updateAttribute::getDisplayExpression, reference::setDisplayExpression);
    }

    private <T> void setValueIfPresent(Supplier<UpdateValue<T>> updAttrValueGetter, Consumer<T> attrValueSetter) {
        UpdateValue<T> value = updAttrValueGetter.get();
        if (value != null) {
            attrValueSetter.accept(value.isPresent() ? value.get() : null);
        }
    }

    /**
     * Обновление отображаемого значения ссылки во всех записях с заполненным значением ссылки.
     *
     * @param draftEntity сущность-черновик
     * @param reference   атрибут-ссылка
     */
    private void refreshReferenceDisplayValues(RefBookVersionEntity draftEntity, Structure.Reference reference) {

        RefBookVersionEntity publishedEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(reference.getReferenceCode(), RefBookVersionStatus.PUBLISHED);
        if (publishedEntity == null)
            return;

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(publishedEntity.getStructure());
        if (referenceAttribute == null)
            return;

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

        if (attribute.isReferenceType())
            structure.getReferences().remove(structure.getReference(attributeCode));
        structure.getAttributes().remove(attribute);
        mutateCatchNotUniqueAndRethrow(() -> draftDataService.deleteField(draftEntity.getStorageCode(), attributeCode));
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
                                            RefBookVersionAttribute oldVersionAttribute,
                                            RefBookVersionAttribute newVersionAttribute,
                                            List<AttributeValidation> validations) {

        String attributeCode = newVersionAttribute.getAttribute().getCode();
        Structure structure = versionEntity.getStructure();

        versionValidation.validateAttributeExists(versionEntity.getId(), structure, attributeCode);

        List<AttributeValidationEntity> validationEntities = validations.stream()
                .map(validation -> new AttributeValidationEntity(versionEntity, attributeCode, validation.getType(),
                        validation.valuesToString())).collect(toList());

        boolean skipReferenceValidation = false;
        if (newVersionAttribute.getAttribute().isReferenceType()
                && Objects.nonNull(newVersionAttribute.getReference())
                && Objects.nonNull(oldVersionAttribute)
                && oldVersionAttribute.getAttribute().isReferenceType()) {
            skipReferenceValidation = Objects.nonNull(oldVersionAttribute.getReference())
                    && !Objects.equals(newVersionAttribute.getReference().getDisplayExpression(),
                    oldVersionAttribute.getReference().getDisplayExpression())
                    && conflictRepository.existsByReferrerVersionIdAndRefFieldCodeAndConflictType(
                            versionEntity.getId(), attributeCode, ConflictType.DISPLAY_DAMAGED);
        }

        validateVersionData(versionEntity, skipReferenceValidation, validationEntities);

        deleteAttributeValidation(versionEntity.getId(), attributeCode, null);

        attributeValidationRepository.saveAll(validationEntities);

        conflictRepository.deleteByReferrerVersionIdAndRefFieldCodeAndRefRecordIdIsNull(versionEntity.getId(), attributeCode);
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
        auditLogService.addAction(AuditAction.DRAFT_EDITING, () -> refBook, Map.of(action, payload));
    }
}