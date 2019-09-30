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
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
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
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.RowValuePage;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.inovus.ms.rdm.api.model.version.*;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.api.service.VersionFileService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.util.FileNameGenerator;
import ru.inovus.ms.rdm.api.util.StructureUtils;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
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
import ru.inovus.ms.rdm.impl.validation.PrimaryKeyUniqueValidation;
import ru.inovus.ms.rdm.impl.validation.ReferenceValidation;
import ru.inovus.ms.rdm.impl.validation.VersionValidationImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.isPublished;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.isVersionOfRefBook;

@Primary
@Service
public class DraftServiceImpl implements DraftService {

    private static final String ILLEGAL_CREATE_ATTRIBUTE_EXCEPTION_CODE = "Can not update structure, illegal create attribute";
    private static final String ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE = "Can not update structure, illegal update attribute";
    private static final String INCOMPATIBLE_NEW_STRUCTURE_EXCEPTION_CODE = "incompatible.new.structure";
    private static final String INCOMPATIBLE_NEW_TYPE_EXCEPTION_CODE = "incompatible.new.type";
    private static final String ROW_NOT_UNIQUE_EXCEPTION_CODE = "row.not.unique";
    private static final String ROW_IS_EMPTY_EXCEPTION_CODE = "row.is.empty";
    private static final String REQUIRED_FIELD_EXCEPTION_CODE = "validation.required.err";
    private static final String REFERENCE_REFERRED_ATTRIBUTE_NOT_FOUND = "reference.referred.attribute.not.found";
    private static final String REFERENCE_REFERRED_ATTRIBUTES_NOT_FOUND = "reference.referred.attributes.not.found";

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

    private int errorCountLimit = 100;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public DraftServiceImpl(RefBookVersionRepository versionRepository, RefBookConflictRepository conflictRepository,
                            DraftDataService draftDataService, DropDataService dropDataService, SearchDataService searchDataService,
                            RefBookService refBookService, RefBookLockService refBookLockService, VersionService versionService,
                            FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                            VersionFileService versionFileService,
                            VersionValidation versionValidation,
                            PassportValueRepository passportValueRepository, AttributeValidationRepository attributeValidationRepository) {
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

        refBookLockService.setRefBookUploading(refBookId);
        try {
            Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());

            String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
            switch (extension) {
                case "XLSX": return updateDraftDataByXlsx(refBookId, fileModel, inputStreamSupplier);
                case "XML": return updateDraftDataByXml(refBookId, fileModel, inputStreamSupplier);
                default: throw new RdmException("invalid file extension");
            }

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

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
            RefBookVersionEntity lastRefBookVersion = getLastRefBookVersion(refBookId);
            RefBookVersionEntity draftVersion = getDraftByRefBook(refBookId);
            if (draftVersion == null && lastRefBookVersion == null)
                throw new NotFoundException(new Message(VersionValidationImpl.REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

            // NB: structure == null means that draft was created during passport saving
            if (draftVersion != null && draftVersion.getStructure() != null) {
                dropDataService.drop(singleton(draftVersion.getStorageCode()));
                versionRepository.deleteById(draftVersion.getId());

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
        final Structure structure = createDraftRequest.getStructure();
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

        RefBookVersionEntity draft = versionRepository.getOne(draftId);

        RowsValidator validator = new RowsValidatorImpl(versionService, searchDataService,
                draft.getStructure(), draft.getStorageCode(), errorCountLimit, false,
                attributeValidationRepository.findAllByVersionId(draftId)
        );
        validator.append(new NonStrictOnTypeRowMapper(draft.getStructure(), versionRepository).map(row));
        validator.process();

        if (row.getData().values().stream().allMatch(ObjectUtils::isEmpty))
            throw new UserException(new Message(ROW_IS_EMPTY_EXCEPTION_CODE));

        RowValue rowValue = ConverterUtil.rowValue(new StructureRowMapper(draft.getStructure(), versionRepository).map(row), draft.getStructure());
        if (rowValue.getSystemId() == null)
            draftDataService.addRows(draft.getStorageCode(), singletonList(rowValue));
        else {
            conflictRepository.deleteByReferrerVersionIdAndRefRecordId(draft.getId(), (Long) rowValue.getSystemId());
            draftDataService.updateRow(draft.getStorageCode(), rowValue);
        }
    }

    @Override
    @Transactional
    public void deleteRow(Integer draftId, Long systemId) {

        versionValidation.validateDraft(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        conflictRepository.deleteByReferrerVersionIdAndRefRecordId(draft.getId(), systemId);
        draftDataService.deleteRows(draft.getStorageCode(), singletonList(systemId));
    }

    @Override
    @Transactional
    public void deleteAllRows(Integer draftId) {

        versionValidation.validateDraft(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(draft.getId());
        draftDataService.deleteAllRows(draft.getStorageCode());
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

        RefBookVersionEntity versionEntity = versionRepository.getOne(draftId);
        return new Draft(versionEntity.getId(), versionEntity.getStorageCode());
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
        validateUpdateAttribute(updateAttribute, attribute, draftEntity.getStorageCode());

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
    }

    private void validateDisplayExpression(String displayExpression, String refBookCode) {

        if (isEmpty(displayExpression))
            return; // NB: to-do: throw exception and fix absent referredBook in testLifecycle.

        RefBookVersion referredVersion = versionService.getLastPublishedVersion(refBookCode);
        List<String> incorrectFields = StructureUtils.getAbsentPlaceholders(displayExpression, referredVersion.getStructure());
        if (!isEmpty(incorrectFields)) {
            if (incorrectFields.size() == 1)
                throw new UserException(new Message(REFERENCE_REFERRED_ATTRIBUTE_NOT_FOUND, incorrectFields.get(0)));

            String incorrectCodes = String.join("\",\"", incorrectFields);
            throw new UserException(new Message(REFERENCE_REFERRED_ATTRIBUTES_NOT_FOUND, incorrectCodes));
        }
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
            Structure.Reference reference;
            if (FieldType.REFERENCE.equals(oldType)) {
                reference = structure.getReference(updateAttribute.getCode());
            } else {
                reference = new Structure.Reference();
            }

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

    @SuppressWarnings({"squid:S1067", "squid:S3776"})
    private void validateUpdateAttribute(UpdateAttribute updateAttribute, Structure.Attribute attribute, String storageCode) {
        if (attribute == null
                || updateAttribute.getVersionId() == null
                || updateAttribute.getType() == null)
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);

        if (updateAttribute.isReferenceType() &&
                ((attribute.isReferenceType() && isValidUpdateReferenceValues(updateAttribute, this::isUpdateValueNotNullAndEmpty))
                        || (!attribute.isReferenceType() && isValidUpdateReferenceValues(updateAttribute, this::isUpdateValueNullOrEmpty))
                ))
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);

        // проверка отсутствия пустых значений в поле при установке первичного ключа
        if (!isUpdateValueNullOrEmpty(updateAttribute.getIsPrimary()) && updateAttribute.getIsPrimary().get()
                && draftDataService.isFieldContainEmptyValues(storageCode, updateAttribute.getCode()))
            throw new UserException(new Message(INCOMPATIBLE_NEW_STRUCTURE_EXCEPTION_CODE, attribute.getName()));

        if (!isUpdateValueNullOrEmpty(updateAttribute.getIsPrimary()) && updateAttribute.getIsPrimary().get()) {
            validatePrimaryKeyUnique(storageCode, updateAttribute);
        }

        // проверка совместимости типов, если столбец не пустой и изменяется тип. Если пустой - можно изменить тип
        if (draftDataService.isFieldNotEmpty(storageCode, updateAttribute.getCode())) {
            if (!isCompatibleTypes(attribute.getType(), updateAttribute.getType())) {
                throw new UserException(new Message(INCOMPATIBLE_NEW_TYPE_EXCEPTION_CODE, attribute.getName()));
            }
        } else
            return;

        if (updateAttribute.isReferenceType() && !attribute.isReferenceType()) {
            validateReferenceValues(updateAttribute);
        }
    }

    private void validatePrimaryKeyUnique(String storageCode, UpdateAttribute updateAttribute) {
        List<Message> pkValidationMessages = new PrimaryKeyUniqueValidation(draftDataService, storageCode,
                singletonList(updateAttribute.getCode())).validate();
        if (pkValidationMessages != null && !pkValidationMessages.isEmpty())
            throw new UserException(pkValidationMessages);
    }

    private void validateReferenceValues(UpdateAttribute updateAttribute) {
        List<Message> referenceValidationMessages = new ReferenceValidation(
                searchDataService,
                versionRepository,
                new Structure.Reference(updateAttribute.getAttribute().get(), updateAttribute.getReferenceCode().get(), updateAttribute.getDisplayExpression().get()),
                updateAttribute.getVersionId()).validate();
        if (!isEmpty(referenceValidationMessages))
            throw new UserException(referenceValidationMessages);
    }

    private boolean isCompatibleTypes(FieldType realDataType, FieldType newDataType) {
        return realDataType.equals(newDataType) || STRING.equals(realDataType) || STRING.equals(newDataType);
    }

    private boolean isValidUpdateReferenceValues(UpdateAttribute updateAttribute, Function<UpdateValue, Boolean> valueValidateFunc) {
        return valueValidateFunc.apply(updateAttribute.getReferenceCode())
                || valueValidateFunc.apply(updateAttribute.getAttribute());
    }

    private boolean isUpdateValueNotNullAndEmpty(UpdateValue updateValue) {
        return updateValue != null && !updateValue.isPresent();
    }

    private boolean isUpdateValueNullOrEmpty(UpdateValue updateValue) {
        return updateValue == null || !updateValue.isPresent();
    }

    /**
     * Обновление отображаемого значения ссылки во всех записях с заполненным значением ссылки.
     *
     * @param draftEntity сущность-черновик
     * @param reference   атрибут-ссылка
     */
    // NB: may-be: Move to `ReferenceServiceImpl`.
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
            if (type == null) {
                validations = attributeValidationRepository.findAllByVersionIdAndAttribute(draftId, attribute);
            } else {
                validations = attributeValidationRepository.findAllByVersionIdAndAttributeAndType(draftId, attribute, type);
            }
        }

        if (!validations.isEmpty())
            attributeValidationRepository.deleteAll(validations);
    }

    @Override
    public List<AttributeValidation> getAttributeValidations(Integer draftId, String attribute) {
        List<AttributeValidationEntity> validationEntities;
        if (attribute == null) {
            validationEntities = attributeValidationRepository.findAllByVersionId(draftId);
        } else {
            validationEntities = attributeValidationRepository.findAllByVersionIdAndAttribute(draftId, attribute);
        }
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
}