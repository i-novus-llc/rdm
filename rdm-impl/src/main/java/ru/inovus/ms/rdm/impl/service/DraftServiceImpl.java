package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import ru.i_novus.components.common.exception.CodifiedException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;
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
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.RowValuePage;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.inovus.ms.rdm.api.model.version.*;
import ru.inovus.ms.rdm.api.service.*;
import ru.inovus.ms.rdm.api.util.FileNameGenerator;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.*;
import ru.inovus.ms.rdm.impl.file.FileStorage;
import ru.inovus.ms.rdm.impl.file.NonStrictOnTypeRowMapper;
import ru.inovus.ms.rdm.impl.file.PlainRowMapper;
import ru.inovus.ms.rdm.impl.file.StructureRowMapper;
import ru.inovus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.inovus.ms.rdm.impl.file.process.*;
import ru.inovus.ms.rdm.impl.repository.AttributeValidationRepository;
import ru.inovus.ms.rdm.impl.repository.PassportValueRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.util.ConverterUtil;
import ru.inovus.ms.rdm.impl.util.ModelGenerator;
import ru.inovus.ms.rdm.impl.util.RowDiff;
import ru.inovus.ms.rdm.impl.util.RowDiffUtils;
import ru.inovus.ms.rdm.impl.validation.AttributeUpdateValidator;
import ru.inovus.ms.rdm.impl.validation.VersionValidationImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.isPublished;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.isVersionOfRefBook;

@Primary
@Service
@SuppressWarnings("squid:S00104")
public class DraftServiceImpl implements DraftService {

    private static final String ILLEGAL_CREATE_ATTRIBUTE_EXCEPTION_CODE = "Can not update structure, illegal create attribute";
    private static final String ROW_NOT_UNIQUE_EXCEPTION_CODE = "row.not.unique";
    private static final String ROW_IS_EMPTY_EXCEPTION_CODE = "row.is.empty";
    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";
    private static final String REQUIRED_FIELD_EXCEPTION_CODE = "validation.required.err";

    private RefBookVersionRepository versionRepository;
    private RefBookConflictRepository conflictRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;
    private SearchDataService searchDataService;

    private RefBookService refBookService;
    private RefBookLockService refBookLockService;
    private VersionService versionService;

    private FileStorage fileStorage;
    private FileNameGenerator fileNameGenerator;
    private VersionFileService versionFileService;

    private VersionValidation versionValidation;

    private PassportValueRepository passportValueRepository;

    private AttributeValidationRepository attributeValidationRepository;

    private AuditLogService auditLogService;
    private AttributeUpdateValidator attributeUpdateValidator;

    private int errorCountLimit = 100;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public DraftServiceImpl(RefBookVersionRepository versionRepository, RefBookConflictRepository conflictRepository,
                            DraftDataService draftDataService, DropDataService dropDataService, SearchDataService searchDataService,
                            RefBookService refBookService, RefBookLockService refBookLockService, VersionService versionService,
                            FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                            VersionFileService versionFileService,
                            VersionValidation versionValidation,
                            PassportValueRepository passportValueRepository, AttributeValidationRepository attributeValidationRepository,
                            AuditLogService auditLogService, AttributeUpdateValidator attributeUpdateValidator) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
        this.searchDataService = searchDataService;

        this.refBookService = refBookService;
        this.refBookLockService = refBookLockService;
        this.versionService = versionService;

        this.fileStorage = fileStorage;
        this.fileNameGenerator = fileNameGenerator;
        this.versionFileService = versionFileService;

        this.versionValidation = versionValidation;

        this.passportValueRepository = passportValueRepository;
        this.attributeValidationRepository = attributeValidationRepository;
        this.auditLogService = auditLogService;
        this.attributeUpdateValidator = attributeUpdateValidator;
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
        refBookLockService.setRefBookUploading(refBookId);
        try {
            draft = updateDraftDataByFile(refBookId, fileModel);

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        auditLogService.addAction(AuditAction.UPLOAD_VERSION_FROM_FILE, () -> versionRepository.getOne(draft.getId()));

        return draft;
    }

    @Override
    @Transactional(timeout = 1200000)
    public Draft create(FileModel fileModel) {
        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        switch (extension) {
            case "XLSX": return createByXlsx(fileModel);
            case "XML": return createByXml(fileModel);
            default: throw new RdmException("invalid file extension");
        }
    }

    @SuppressWarnings("unused")
    private Draft createByXlsx(FileModel fileModel) {
        throw new RdmException("creating draft from xlsx is not implemented yet");
    }

    private Draft createByXml(FileModel fileModel) {
        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());
        try(XmlCreateRefBookFileProcessor createRefBookFileProcessor = new XmlCreateRefBookFileProcessor(refBookService)) {
            RefBook refBook = createRefBookFileProcessor.process(inputStreamSupplier);
            return updateDraftDataByXml(refBook.getRefBookId(), fileModel, inputStreamSupplier);
        }
    }

    /** Обновление данных черновика справочника из файла.
     *
     * @param refBookId идентификатор справочника
     * @param fileModel файл
     * @return Черновик справочника
     */
    private Draft updateDraftDataByFile(Integer refBookId, FileModel fileModel) {

        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());

        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        switch (extension) {
            case "XLSX": return updateDraftDataByXlsx(refBookId, fileModel, inputStreamSupplier);
            case "XML": return updateDraftDataByXml(refBookId, fileModel, inputStreamSupplier);
            default: throw new RdmException("invalid file extension");
        }
    }

    private Draft updateDraftDataByXlsx(Integer refBookId, FileModel fileModel, Supplier<InputStream> inputStreamSupplier) {
        BiConsumer<String, Structure> saveDraftConsumer = getSaveDraftConsumer(refBookId);
        RowsProcessor rowsProcessor = new CreateDraftBufferedRowsPersister(draftDataService, saveDraftConsumer);

        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        try (FilePerRowProcessor persister = FileProcessorFactory.createProcessor(extension,
                rowsProcessor, new PlainRowMapper())) {
            persister.process(inputStreamSupplier);

        } catch (IOException e) {
            throw new RdmException(e);
        }

        RefBookVersionEntity createdDraft = getDraftByRefBook(refBookId);
        return new Draft(createdDraft.getId(), createdDraft.getStorageCode());
    }

    private Draft updateDraftDataByXml(Integer refBookId, FileModel fileModel, Supplier<InputStream> inputStreamSupplier) {
        try(XmlUpdateDraftFileProcessor xmlUpdateDraftFileProcessor = new XmlUpdateDraftFileProcessor(refBookId, this)) {
            Draft draft = xmlUpdateDraftFileProcessor.process(inputStreamSupplier);
            updateDraftData(versionRepository.getOne(draft.getId()), fileModel);
            return draft;
        }
    }

    /** Обновление данных черновика из файла.
     *
     * @param draft     черновик
     * @param fileModel файл
     */
    private void updateDraftData(RefBookVersionEntity draft, FileModel fileModel) {

        String storageCode = draft.getStorageCode();
        Structure structure = draft.getStructure();

        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());

        StructureRowMapper nonStrictOnTypeRowMapper = new NonStrictOnTypeRowMapper(structure, versionRepository);
        try (FilePerRowProcessor validator = FileProcessorFactory
                .createProcessor(extension,
                        new RowsValidatorImpl(versionService, searchDataService,
                                structure, storageCode, errorCountLimit, false,
                                attributeValidationRepository.findAllByVersionId(draft.getId())
                        ),
                        nonStrictOnTypeRowMapper)) {
            validator.process(inputStreamSupplier);

        } catch (IOException e) {
            throw new RdmException(e);
        }

        StructureRowMapper structureRowMapper = new StructureRowMapper(structure, versionRepository);
        try (FilePerRowProcessor persister = FileProcessorFactory
                .createProcessor(extension,
                        new BufferedRowsPersister(draftDataService, storageCode, structure),
                        structureRowMapper)) {
            persister.process(inputStreamSupplier);

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
                    .map(entry -> new PassportValueEntity(new PassportAttributeEntity(entry.getKey()), entry.getValue(), null))
                    .collect(toList());
        }

        final Structure structure = createDraftRequest.getStructure();
        List<Field> fields = ConverterUtil.fields(structure);
        if (draftVersion == null) {
            draftVersion = newDraftVersion(structure, passportValues != null ? passportValues : lastRefBookVersion.getPassportValues());
            draftVersion.setRefBook(newRefBook(refBookId));
            String draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);

        } else {
            draftVersion = updateDraft(structure, draftVersion, fields, passportValues);
        }

        RefBookVersionEntity savedDraftVersion = versionRepository.save(draftVersion);
        return new Draft(savedDraftVersion.getId(), savedDraftVersion.getStorageCode());
    }

    @Override
    @Transactional
    public Draft createFromVersion(Integer versionId) {

        versionValidation.validateVersion(versionId);
        RefBookVersionEntity sourceVersion = versionRepository.getOne(versionId);

        Map<String, String> passport = new HashMap<>();
        sourceVersion.getPassportValues().forEach(passportValueEntity -> passport.put(passportValueEntity.getAttribute().getCode(), passportValueEntity.getValue()));
        CreateDraftRequest draftRequest  = new CreateDraftRequest(sourceVersion.getRefBook().getId(), sourceVersion.getStructure(), passport);
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

            if(passportValues == null) passportValues = draftVersion.getPassportValues();

            dropDataService.drop(singleton(draftCode));
            versionRepository.deleteById(draftVersion.getId());

            draftVersion = newDraftVersion(structure, passportValues);
            draftVersion.setRefBook(newRefBook(refBookId));
            draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);

        } else {
            passportValueRepository.deleteInBatch(draftVersion.getPassportValues());
            draftDataService.deleteAllRows(draftCode);

            if(passportValues != null) draftVersion.setPassportValues(passportValues);
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

    @Override
    @Transactional
    public void updateData(Integer draftId, Row row) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);

        if (isEmptyRow(row))
            throw new UserException(new Message(ROW_IS_EMPTY_EXCEPTION_CODE));

        validateDataByStructure(draftVersion, singletonList(row));

        StructureRowMapper rowMapper = new StructureRowMapper(draftVersion.getStructure(), versionRepository);
        RowValue newRowValue = ConverterUtil.rowValue(rowMapper.map(row), draftVersion.getStructure());

        if (newRowValue.getSystemId() == null) {
            draftDataService.addRows(draftVersion.getStorageCode(), singletonList(newRowValue));

            auditEditData(draftVersion, "create_row", newRowValue.getFieldValues());

        } else {
            List<String> fields = draftVersion.getStructure().getAttributes().stream().map(Structure.Attribute::getCode).collect(toList());
            RowValue oldRowValue = searchDataService.findRow(draftVersion.getStorageCode(), fields, newRowValue.getSystemId());
            if (isNotSystemIdRowValue(newRowValue.getSystemId(), oldRowValue))
                throw new UserException(new Message(ROW_NOT_FOUND_EXCEPTION_CODE, newRowValue.getSystemId()));

            RowDiff rowDiff = RowDiffUtils.getRowDiff(oldRowValue, newRowValue);

            conflictRepository.deleteByReferrerVersionIdAndRefRecordId(draftVersion.getId(), (Long) newRowValue.getSystemId());
            draftDataService.updateRow(draftVersion.getStorageCode(), newRowValue);

            auditEditData(draftVersion, "update_row", rowDiff);
        }
    }

    private boolean isNotSystemIdRowValue(Object systemId, RowValue rowValue) {
        return rowValue == null || !systemId.equals(rowValue.getSystemId());
    }

    @Override
    @Transactional
    public void updateData(Integer draftId, List<Row> rows) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);

        if (isEmpty(rows))
            throw new UserException(new Message(ROW_IS_EMPTY_EXCEPTION_CODE));

        rows = rows.stream().filter(row -> !isEmptyRow(row)).collect(toList());
        if (isEmpty(rows))
            throw new UserException(new Message(ROW_IS_EMPTY_EXCEPTION_CODE));

        validateDataByStructure(draftVersion, rows);

        StructureRowMapper rowMapper = new StructureRowMapper(draftVersion.getStructure(), versionRepository);
        List<RowValue> newRowValues = rows.stream()
                .map(row -> ConverterUtil.rowValue(rowMapper.map(row), draftVersion.getStructure()))
                .collect(toList());

        List<RowValue> addedRowValues = newRowValues.stream().filter(rowValue -> rowValue.getSystemId() == null).collect(toList());
        if (!isEmpty(addedRowValues)) {
            draftDataService.addRows(draftVersion.getStorageCode(), addedRowValues);

            List<Object> addedData = addedRowValues.stream().map(RowValue::getFieldValues).collect(toList());
            auditEditData(draftVersion, "create_rows", addedData);
        }

        List<RowValue> updatedRowValues = newRowValues.stream().filter(rowValue -> rowValue.getSystemId() != null).collect(toList());
        if (!isEmpty(updatedRowValues)) {
            List<String> fields = draftVersion.getStructure().getAttributes().stream().map(Structure.Attribute::getCode).collect(toList());
            List<Object> systemIds = updatedRowValues.stream().map(RowValue::getSystemId).collect(toList());
            List<RowValue> oldRowValues = searchDataService.findRows(draftVersion.getStorageCode(), fields, systemIds);

            List<Message> messages = systemIds.stream()
                    .filter(systemId -> isNotSystemIdRowValue(systemId, oldRowValues))
                    .map(systemId -> new Message(ROW_NOT_FOUND_EXCEPTION_CODE, systemId))
                    .collect(toList());
            if (!isEmpty(messages))
                throw new UserException(messages);

            List<RowDiff> rowDiffs = oldRowValues.stream()
                    .map(oldRowValue -> {
                        RowValue newRowValue = getSystemIdRowValue(oldRowValue.getSystemId(), updatedRowValues);
                        return RowDiffUtils.getRowDiff(oldRowValue, newRowValue);
                    })
                    .collect(toList());

            conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(draftVersion.getId(),
                    systemIds.stream().map(systemId -> (Long) systemId).collect(toList())
            );
            draftDataService.updateRows(draftVersion.getStorageCode(), updatedRowValues);

            auditEditData(draftVersion, "update_rows", rowDiffs);
        }
    }

    private boolean isNotSystemIdRowValue(Object systemId, List<RowValue> rowValues) {
        return isEmpty(rowValues) ||
                rowValues.stream().noneMatch(rowValue -> systemId.equals(rowValue.getSystemId()));
    }

    private RowValue getSystemIdRowValue(Object systemId, List<RowValue> rowValues) {
        return rowValues.stream()
                .filter(rowValue -> systemId.equals(rowValue.getSystemId()))
                .findFirst().orElse(null);
    }

    /** Валидация добавляемых/обновляемых строк данных по структуре. */
    private void validateDataByStructure(RefBookVersionEntity draftVersion, List<Row> rows) {

        if (isEmpty(rows))
            return;

        RowsValidator validator = new RowsValidatorImpl(versionService, searchDataService,
                draftVersion.getStructure(), draftVersion.getStorageCode(), errorCountLimit, false,
                attributeValidationRepository.findAllByVersionId(draftVersion.getId())
        );

        NonStrictOnTypeRowMapper nonStrictOnTypeRowMapper = new NonStrictOnTypeRowMapper(draftVersion.getStructure(), versionRepository);
        rows.forEach(row -> validator.append(nonStrictOnTypeRowMapper.map(row)));
        validator.process();
    }

    /** Проверка строки данных на наличие значений. */
    private boolean isEmptyRow(Row row) {
        return row == null ||
                row.getData().values().stream().allMatch(ObjectUtils::isEmpty);
    }

    @Override
    @Transactional
    public void deleteRow(Integer draftId, Long systemId) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);

        conflictRepository.deleteByReferrerVersionIdAndRefRecordId(draftVersion.getId(), systemId);
        draftDataService.deleteRows(draftVersion.getStorageCode(), singletonList(systemId));

        auditEditData(draftVersion, "delete_row", systemId);
    }

    @Override
    @Transactional
    public void deleteAllRows(Integer draftId) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);

        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draftVersion.getId());
        draftDataService.deleteAllRows(draftVersion.getStorageCode());

        auditEditData(draftVersion, "delete_all_rows", "-");
    }

    @Override
    public void updateData(Integer draftId, FileModel fileModel) {

        versionValidation.validateDraft(draftId);
        RefBookVersionEntity draftVersion = versionRepository.findById(draftId).orElseThrow();

        Integer refBookId = draftVersion.getRefBook().getId();
        refBookLockService.setRefBookUploading(refBookId);
        try {
            updateDraftData(draftVersion, fileModel);

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        auditLogService.addAction(AuditAction.UPLOAD_DATA, () -> draftVersion);
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
                .findAll(isPublished().and(isVersionOfRefBook(refBookId)),
                        PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "fromDate")));
        return lastPublishedVersions.hasContent() ? lastPublishedVersions.getContent().get(0) : null;
    }

    @Override
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
        if (isReference != attribute.isReferenceType())
            throw new IllegalArgumentException(ILLEGAL_CREATE_ATTRIBUTE_EXCEPTION_CODE);

        if (isReference) {
            validateDisplayExpression(reference.getDisplayExpression(), reference.getReferenceCode());
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
                throw new UserException(new Message(REQUIRED_FIELD_EXCEPTION_CODE, attribute.getName()));
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
                validateDisplayExpression(newDisplayExpression, updateAttribute.getReferenceCode().get());
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

    private void validateDisplayExpression(String displayExpression, String refBookCode) {

        if (isEmpty(displayExpression))
            return; // NB: to-do: throw exception and fix absent referredBook in testLifecycle.

        RefBookVersion referredVersion = versionService.getLastPublishedVersion(refBookCode);
        versionValidation.validateReferenceDisplayExpression(displayExpression, referredVersion);
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

        try {
            draftDataService.deleteField(draftEntity.getStorageCode(), attributeCode);

        } catch (NotUniqueException e) {
            throw new UserException(ROW_NOT_UNIQUE_EXCEPTION_CODE, e);
        }

        attributeValidationRepository.deleteAll(
                attributeValidationRepository.findAllByVersionIdAndAttribute(draftId, attributeCode));
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
        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setCode(refBookCode);
        criteria.setExcludeDraft(false);
        Page<RefBook> search = refBookService.search(criteria);
        return search.stream().findFirst().map(RefBook::getDraftVersionId).orElse(null);
    }

    private void auditStructureEdit(RefBookVersionEntity refBook, String action, Structure.Attribute attribute) {
        auditLogService.addAction(AuditAction.EDIT_STRUCTURE, () -> refBook, Map.of(action, attribute));
    }

    private void auditEditData(RefBookVersionEntity refBook, String action, Object payload) {
        auditLogService.addAction(AuditAction.DRAFT_EDITING, () -> refBook, Map.of(action, payload));
    }
}