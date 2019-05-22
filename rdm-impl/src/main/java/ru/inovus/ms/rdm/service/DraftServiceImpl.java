package ru.inovus.ms.rdm.service;

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
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.model.value.*;
import ru.i_novus.platform.datastorage.temporal.service.*;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.file.*;
import ru.inovus.ms.rdm.file.export.*;
import ru.inovus.ms.rdm.file.process.*;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.validation.*;
import ru.inovus.ms.rdm.repositiory.*;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.*;
import ru.inovus.ms.rdm.validation.PrimaryKeyUniqueValidation;
import ru.inovus.ms.rdm.validation.ReferenceValidation;
import ru.kirkazan.common.exception.CodifiedException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;
import static ru.inovus.ms.rdm.util.ConverterUtil.*;

@Primary
@Service
public class DraftServiceImpl implements DraftService {

    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    private VersionService versionService;

    private SearchDataService searchDataService;

    private DropDataService dropDataService;

    private FileStorage fileStorage;

    private FileNameGenerator fileNameGenerator;

    private VersionFileRepository versionFileRepository;

    private VersionNumberStrategy versionNumberStrategy;

    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    private PassportValueRepository passportValueRepository;

    private RefBookLockService refBookLockService;

    private AttributeValidationRepository attributeValidationRepository;

    private PerRowFileGeneratorFactory fileGeneratorFactory;

    private RefBookService refBookService;

    private int errorCountLimit = 100;
    private String passportFileHead = "fullName";
    private boolean includePassport = false;

    private static final String ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE = "Can not update structure, illegal update attribute";
    private static final String INCOMPATIBLE_NEW_STRUCTURE_EXCEPTION_CODE = "incompatible.new.structure";
    private static final String INCOMPATIBLE_NEW_TYPE_EXCEPTION_CODE = "incompatible.new.type";
    private static final String DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "draft.attribute.not.found";
    private static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";
    private static final String REFBOOK_IS_ARCHIVED_EXCEPTION_CODE = "refbook.is.archived";
    private static final String INVALID_VERSION_NAME_EXCEPTION_CODE = "invalid.version.name";
    private static final String INVALID_VERSION_PERIOD_EXCEPTION_CODE = "invalid.version.period";
    private static final String ROW_NOT_UNIQUE_EXCEPTION_CODE = "row.not.unique";
    private static final String ROW_IS_EMPTY_EXCEPTION_CODE = "row.is.empty";
    private static final String REQUIRED_FIELD_EXCEPTION_CODE = "validation.required.err";

    @Autowired
    @SuppressWarnings("all")
    public DraftServiceImpl(DraftDataService draftDataService, RefBookVersionRepository versionRepository, VersionService versionService,
                            SearchDataService searchDataService, DropDataService dropDataService, FileStorage fileStorage,
                            FileNameGenerator fileNameGenerator, VersionFileRepository versionFileRepository, VersionNumberStrategy versionNumberStrategy,
                            VersionPeriodPublishValidation versionPeriodPublishValidation, PassportValueRepository passportValueRepository,
                            RefBookLockService refBookLockService, AttributeValidationRepository attributeValidationRepository,
                            PerRowFileGeneratorFactory fileGeneratorFactory, RefBookService refBookService) {
        this.draftDataService = draftDataService;
        this.versionRepository = versionRepository;
        this.versionService = versionService;
        this.searchDataService = searchDataService;
        this.dropDataService = dropDataService;
        this.fileStorage = fileStorage;
        this.fileNameGenerator = fileNameGenerator;
        this.versionFileRepository = versionFileRepository;
        this.versionNumberStrategy = versionNumberStrategy;
        this.versionPeriodPublishValidation = versionPeriodPublishValidation;
        this.passportValueRepository = passportValueRepository;
        this.refBookLockService = refBookLockService;
        this.attributeValidationRepository = attributeValidationRepository;
        this.fileGeneratorFactory = fileGeneratorFactory;
        this.refBookService = refBookService;
    }

    @Value("${rdm.validation-errors-count}")
    public void setErrorCountLimit(int errorCountLimit) {
        this.errorCountLimit = errorCountLimit;
    }

    @Value("${rdm.download.passport.head}")
    public void setPassportFileHead(String passportFileHead) {
        this.passportFileHead = passportFileHead;
    }

    @Value("${rdm.download.passport-enable}")
    public void setIncludePassport(boolean includePassport) {
        this.includePassport = includePassport;
    }

    @Override
    @Transactional(timeout = 1200000)
    public Draft create(Integer refBookId, FileModel fileModel) {

        validateRefBook(refBookId);

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
            refBookLockService.deleteRefBookAction(refBookId);
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

    /** Обновление данных черновика.
     *
     * @param draft
     * @param fileModel
     */
    private void updateDraftData(RefBookVersionEntity draft, FileModel fileModel) {

        String storageCode = draft.getStorageCode();
        Structure structure = draft.getStructure();

        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());

        StructureRowMapper nonStrictOnTypeRowMapper = new NonStrictOnTypeRowMapper(structure, versionRepository);
        try (FilePerRowProcessor validator = FileProcessorFactory
                .createProcessor(extension,
                        new RowsValidatorImpl(versionService, searchDataService, structure, storageCode, errorCountLimit,
                                attributeValidationRepository.findAllByVersionId(draft.getId())),
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
                throw new NotFoundException(new Message(REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));

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
        validateRefBook(refBookId);

        RefBookVersionEntity lastRefBookVersion = getLastRefBookVersion(refBookId);
        RefBookVersionEntity draftVersion = getDraftByRefBook(refBookId);
        if (draftVersion == null && lastRefBookVersion == null)
            throw new CodifiedException("invalid refbook");

        List<PassportValueEntity> passportValues = null;
        if (createDraftRequest.getPassport() != null) {
            passportValues = createDraftRequest.getPassport()
                    .entrySet()
                    .stream()
                    .map(entry -> new PassportValueEntity(new PassportAttributeEntity(entry.getKey()), entry.getValue(), null))
                    .collect(toList());
        }

        List<Field> fields = fields(structure);
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

        validateVersionExists(versionId);
        RefBookVersionEntity sourceVersion = versionRepository.getOne(versionId);

        Map<String, String> passport = new HashMap<>();
        sourceVersion.getPassportValues().forEach(passportValueEntity -> passport.put(passportValueEntity.getAttribute().getCode(), passportValueEntity.getValue()));
        CreateDraftRequest draftRequest  = new CreateDraftRequest(sourceVersion.getRefBook().getId(), sourceVersion.getStructure(), passport);
        Draft draft = create(draftRequest);

        draftDataService.loadData(draft.getStorageCode(), sourceVersion.getStorageCode(), sourceVersion.getFromDate(), sourceVersion.getToDate());
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

        validateDraft(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);

        RowsValidator validator = new RowsValidatorImpl(versionService, searchDataService, draft.getStructure(),
                draft.getStorageCode(), errorCountLimit, attributeValidationRepository.findAllByVersionId(draftId));
        validator.append(new NonStrictOnTypeRowMapper(draft.getStructure(), versionRepository).map(row));
        validator.process();

        if (row.getData().values().stream().allMatch(ObjectUtils::isEmpty))
            throw new UserException(new Message(ROW_IS_EMPTY_EXCEPTION_CODE));

        RowValue rowValue = rowValue(new StructureRowMapper(draft.getStructure(), versionRepository).map(row), draft.getStructure());
        if (rowValue.getSystemId() == null)
            draftDataService.addRows(draft.getStorageCode(), singletonList(rowValue));
        else
            draftDataService.updateRow(draft.getStorageCode(), rowValue);
    }

    @Override
    @Transactional
    public void deleteRow(Integer draftId, Long systemId) {

        validateDraft(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        draftDataService.deleteRows(draft.getStorageCode(), singletonList(systemId));
    }

    @Override
    @Transactional
    public void deleteAllRows(Integer draftId) {

        validateDraft(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        draftDataService.deleteAllRows(draft.getStorageCode());
    }

    @Override
    @Transactional(timeout = 1200000)
    public void updateData(Integer draftId, FileModel fileModel) {

        validateDraft(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        Integer refBookId = draft.getRefBook().getId();

        refBookLockService.setRefBookUploading(refBookId);
        try {
            updateDraftData(draft, fileModel);

        } finally {
            refBookLockService.deleteRefBookAction(refBookId);
        }
    }

    @Override
    @Transactional
    public Page<RefBookRowValue> search(Integer draftId, SearchDataCriteria criteria) {

        validateDraftExists(draftId);

        RefBookVersionEntity draft = versionRepository.getOne(draftId);
        String storageCode = draft.getStorageCode();
        List<Field> fields = fields(draft.getStructure());

        DataCriteria dataCriteria = new DataCriteria(storageCode, null, null,
                fields, getFieldSearchCriteriaList(criteria.getAttributeFilter()), criteria.getCommonFilter());
        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return new RowValuePage(pagedData).map(rv -> new RefBookRowValue((LongRowValue) rv, draft.getId()));
    }


    @Override
    public void publish(Integer draftId, String versionName, LocalDateTime fromDate, LocalDateTime toDate) {

        validateDraft(draftId);

        RefBookVersionEntity draftVersion = versionRepository.findById(draftId).orElseThrow();
        Integer refBookId = draftVersion.getRefBook().getId();
        refBookLockService.setRefBookPublishing(refBookId);
        try {
            if (versionName == null) {
                versionName = versionNumberStrategy.next(refBookId);
            } else if (!versionNumberStrategy.check(versionName, refBookId)) {
                throw new UserException(new Message(INVALID_VERSION_NAME_EXCEPTION_CODE, versionName));
            }

            if (fromDate == null) fromDate = TimeUtils.now();
            if (toDate != null && fromDate.isAfter(toDate)) throw new UserException(INVALID_VERSION_PERIOD_EXCEPTION_CODE);

            versionPeriodPublishValidation.validate(fromDate, toDate, refBookId);

            RefBookVersionEntity lastPublishedVersion = getLastPublishedVersion(draftVersion);
            String storageCode = draftDataService.applyDraft(
                    lastPublishedVersion != null ? lastPublishedVersion.getStorageCode() : null,
                    draftVersion.getStorageCode(),
                    fromDate,
                    toDate
            );

            Set<String> dataStorageToDelete = new HashSet<>();
            dataStorageToDelete.add(draftVersion.getStorageCode());

            draftVersion.setStorageCode(storageCode);
            draftVersion.setVersion(versionName);
            draftVersion.setStatus(RefBookVersionStatus.PUBLISHED);
            draftVersion.setFromDate(fromDate);
            draftVersion.setToDate(toDate);
            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId);
            versionRepository.save(draftVersion);

            if (lastPublishedVersion != null && lastPublishedVersion.getStorageCode() != null && draftVersion.getStructure().storageEquals(lastPublishedVersion.getStructure())) {
                dataStorageToDelete.add(lastPublishedVersion.getStorageCode());
                versionRepository.findByStorageCode(lastPublishedVersion.getStorageCode()).stream()
                        .peek(version -> version.setStorageCode(storageCode))
                        .forEach(versionRepository::save);
            }

            dropDataService.drop(dataStorageToDelete);

            RefBookVersion versionModel = versionService.getById(draftId);
            for (FileType fileType : PerRowFileGeneratorFactory.getAvailableTypes())
                saveVersionFile(versionModel, fileType, generateVersionFile(versionModel, fileType));
        } finally {
            refBookLockService.deleteRefBookAction(refBookId);
        }
    }

    private RefBookVersionEntity getLastPublishedVersion(RefBookVersionEntity draftVersion) {
        return versionRepository
                .findFirstByRefBookIdAndStatusOrderByFromDateDesc(draftVersion.getRefBook().getId(), RefBookVersionStatus.PUBLISHED);
    }

    private void resolveOverlappingPeriodsInFuture(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId) {

        if (toDate == null) toDate = MAX_TIMESTAMP;
        Iterable<RefBookVersionEntity> versions = versionRepository.findAll(
                hasOverlappingPeriods(fromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
        );
        versions.forEach(version -> {
            if (fromDate.isAfter(version.getFromDate())) {
                version.setToDate(fromDate);
                versionRepository.save(version);
            } else {
                versionRepository.deleteById(version.getId());
            }
        });
    }

    private RefBookVersionEntity getLastRefBookVersion(Integer refBookId) {
        Page<RefBookVersionEntity> lastPublishedVersions = versionRepository
                .findAll(isPublished().and(isVersionOfRefBook(refBookId)),
                        PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "fromDate")));
        return lastPublishedVersions.hasContent() ? lastPublishedVersions.getContent().get(0) : null;
    }

    @Override
    public void remove(Integer draftId) {

        validateDraft(draftId);

        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        versionRepository.deleteById(draftId);
    }

    @Override
    @Transactional
    public Draft getDraft(Integer draftId) {

        validateDraftExists(draftId);

        RefBookVersionEntity versionEntity = versionRepository.getOne(draftId);
        return new Draft(versionEntity.getId(), versionEntity.getStorageCode());
    }

    @Override
    @Transactional
    public void createAttribute(CreateAttribute createAttribute) {

        validateDraftExists(createAttribute.getVersionId());
        validateDraftNotArchived(createAttribute.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(createAttribute.getVersionId());

        RefBookVersionEntity draftEntity = versionRepository.getOne(createAttribute.getVersionId());
        Structure.Attribute attribute = createAttribute.getAttribute();
        Structure structure = draftEntity.getStructure();
        validateRequired(attribute, draftEntity.getStorageCode(), structure);

        //clear previous primary keys
        if (createAttribute.getAttribute().getIsPrimary())
            structure.clearPrimary();

        Structure.Reference reference = createAttribute.getReference();
        draftDataService.addField(draftEntity.getStorageCode(), field(attribute));

        if (structure == null) {
            structure = new Structure();
        }
        if (structure.getAttributes() == null)
            structure.setAttributes(new ArrayList<>());

        structure.getAttributes().add(attribute);

        if (FieldType.REFERENCE.equals(attribute.getType())) {
            if (structure.getReferences() == null)
                structure.setReferences(new ArrayList<>());
            structure.getReferences().add(reference);
        }
        draftEntity.setStructure(structure);
    }

    private void validateRequired(Structure.Attribute attribute, String storageCode, Structure structure) {
        if (structure != null && structure.getAttributes() != null && attribute.getIsPrimary()) {
            List<RowValue> data = searchDataService.getData(
                    new DataCriteria(storageCode, null, null, fields(structure), emptySet(), null)
            );
            if (!isEmpty(data)) {
                throw new UserException(new Message(REQUIRED_FIELD_EXCEPTION_CODE, attribute.getName()));
            }
        }
    }

    @Override
    @Transactional
    public void updateAttribute(UpdateAttribute updateAttribute) {

        validateDraftExists(updateAttribute.getVersionId());
        validateDraftNotArchived(updateAttribute.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(updateAttribute.getVersionId());

        RefBookVersionEntity draftEntity = versionRepository.getOne(updateAttribute.getVersionId());
        Structure structure = draftEntity.getStructure();
        Structure.Attribute attribute = structure.getAttribute(updateAttribute.getCode());
        validateUpdateAttribute(updateAttribute, attribute, draftEntity.getStorageCode());

        //clear previous primary keys
        if (updateAttribute.getIsPrimary() != null
                && updateAttribute.getIsPrimary().isPresent()
                && updateAttribute.getIsPrimary().get())
            structure.clearPrimary();

        FieldType oldType = attribute.getType();
        setValueIfPresent(updateAttribute::getName, attribute::setName);
        setValueIfPresent(updateAttribute::getDescription, attribute::setDescription);
        setValueIfPresent(updateAttribute::getIsPrimary, attribute::setPrimary);
        attribute.setType(updateAttribute.getType());

        try {
            draftDataService.updateField(draftEntity.getStorageCode(), field(attribute));

        } catch (CodifiedException ce) {
            throw new UserException(ce.getMessage(), ce);
        }

        if (FieldType.REFERENCE.equals(updateAttribute.getType())) {
            Structure.Reference reference;
            if (FieldType.REFERENCE.equals(oldType)) {
                reference = structure.getReference(updateAttribute.getCode());
            } else {
                reference = new Structure.Reference();
            }
            int updatableReferenceIndex = structure.getReferences().indexOf(reference);
            updateReference(updateAttribute, reference);
            if (updatableReferenceIndex >= 0)
                structure.getReferences().set(updatableReferenceIndex, reference);
            else
                structure.getReferences().add(reference);

        } else if (FieldType.REFERENCE.equals(oldType)) {
            structure.getReferences().remove(structure.getReference(updateAttribute.getCode()));
        }

        if (Objects.equals(oldType, updateAttribute.getType()))
            attributeValidationRepository.deleteAll(
                    attributeValidationRepository.findAllByVersionIdAndAttribute(updateAttribute.getVersionId(), updateAttribute.getCode()));
    }

    private void updateReference(UpdateAttribute updateAttribute, Structure.Reference updatableReference) {

        setValueIfPresent(updateAttribute::getAttribute, updatableReference::setAttribute);
        setValueIfPresent(updateAttribute::getReferenceCode, updatableReference::setReferenceCode);
        setValueIfPresent(updateAttribute::getDisplayExpression, updatableReference::setDisplayExpression);
    }

    private <T> void setValueIfPresent(Supplier<UpdateValue<T>> updAttrValueGetter, Consumer<T> attrValueSetter) {
        UpdateValue<T> value = updAttrValueGetter.get();
        if (value != null) {
            attrValueSetter.accept(value.isPresent() ? value.get() : null);
        }
    }

    @SuppressWarnings("all")
    private void validateUpdateAttribute(UpdateAttribute updateAttribute, Structure.Attribute attribute, String storageCode) {
        if (attribute == null
                || updateAttribute.getVersionId() == null
                || updateAttribute.getType() == null)
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);

        if (FieldType.REFERENCE.equals(updateAttribute.getType()) &&
                (FieldType.REFERENCE.equals(attribute.getType()) && isValidUpdateReferenceValues(updateAttribute, this::isUpdateValueNotNullAndEmpty)
                        || (!FieldType.REFERENCE.equals(attribute.getType()) && isValidUpdateReferenceValues(updateAttribute, this::isUpdateValueNullOrEmpty))))
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);

        // проверка отсутствия пустых значений в поле при установке первичного ключа
        if (!isUpdateValueNullOrEmpty(updateAttribute.getIsPrimary()) && updateAttribute.getIsPrimary().get() && draftDataService.isFieldContainEmptyValues(storageCode, updateAttribute.getCode()))
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

        if (FieldType.REFERENCE.equals(updateAttribute.getType()) && !FieldType.REFERENCE.equals(attribute.getType())) {
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

    @Override
    @Transactional
    public void deleteAttribute(Integer draftId, String attributeCode) {

        validateDraft(draftId);

        refBookLockService.validateRefBookNotBusyByVersionId(draftId);

        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);
        Structure.Attribute attribute = draftEntity.getStructure().getAttribute(attributeCode);

        if (FieldType.REFERENCE.equals(attribute.getType()))
            draftEntity.getStructure().getReferences().remove(draftEntity.getStructure().getReference(attributeCode));
        draftEntity.getStructure().getAttributes().remove(attribute);

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
        validateAttributeExists(versionId, attribute);
        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);

        AttributeValidationEntity attributeValidationEntity = new AttributeValidationEntity(versionEntity, attribute,
                attributeValidation.getType(), attributeValidation.valuesToString());
        validateDataBase(versionEntity, singletonList(attributeValidationEntity));

        deleteAttributeValidation(versionId, attribute, attributeValidation.getType());
        attributeValidationRepository.save(attributeValidationEntity);
    }

    @Override
    @Transactional
    public void deleteAttributeValidation(Integer draftId, String attribute, AttributeValidationType type) {
        List<AttributeValidationEntity> validations;
        if (attribute == null) {
            validateDraftExists(draftId);
            validations = attributeValidationRepository.findAllByVersionId(draftId);
        } else {
            validateAttributeExists(draftId, attribute);
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
        List<AttributeValidationEntity> validations;
        if (attribute == null) {
            validations = attributeValidationRepository.findAllByVersionId(draftId);
        } else {
            validations = attributeValidationRepository.findAllByVersionIdAndAttribute(draftId, attribute);
        }
        return validations.stream().map(AttributeValidationEntity::attributeValidationModel).collect(toList());
    }

    @Override
    @Transactional
    public void updateAttributeValidations(Integer versionId, String attribute, List<AttributeValidation> validations) {

        validateAttributeExists(versionId, attribute);
        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        List<AttributeValidationEntity> validationEntities = validations.stream()
                .map(validation -> new AttributeValidationEntity(versionEntity, attribute, validation.getType(),
                        validation.valuesToString())).collect(toList());
        validateDataBase(versionEntity, validationEntities);

        deleteAttributeValidation(versionId, attribute, null);

        attributeValidationRepository.saveAll(validationEntities);
    }

    private void validateDataBase(RefBookVersionEntity versionEntity, List<AttributeValidationEntity> validationEntities) {

        VersionDataIterator iterator = new VersionDataIterator(versionService, singletonList(versionEntity.getId()));
        RowsValidator validator = new RowsValidatorImpl(versionService, searchDataService, versionEntity.getStructure(),
                versionEntity.getStorageCode(), errorCountLimit, validationEntities);

        while (iterator.hasNext()) {
            validator.append(iterator.next());
        }
        validator.process();
    }

    @Override
    @Transactional
    public ExportFile getDraftFile(Integer draftId, FileType fileType) {

        validateDraftExists(draftId);

        RefBookVersion versionModel = ModelGenerator.versionModel(versionRepository.getOne(draftId));

        return new ExportFile(
                generateVersionFile(versionModel, fileType),
                fileNameGenerator.generateZipName(versionModel, fileType));
    }

    private InputStream generateVersionFile(RefBookVersion versionModel, FileType fileType) {
        VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(versionModel.getId()));
        try (FileGenerator fileGenerator = fileGeneratorFactory
                .getFileGenerator(dataIterator, versionModel, fileType);
             Archiver archiver = new Archiver()) {
            if (includePassport) {
                try (FileGenerator passportPdfFileGenerator =
                             new PassportPdfFileGenerator(passportValueRepository, versionModel.getId(), passportFileHead,
                                     versionModel.getCode())) {
                    archiver.addEntry(passportPdfFileGenerator, fileNameGenerator.generateName(versionModel, FileType.PDF));
                }
            }
            return archiver
                    .addEntry(fileGenerator, fileNameGenerator.generateName(versionModel, fileType))
                    .getArchive();
        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    private void saveVersionFile(RefBookVersion version, FileType fileType, InputStream is) {
        try (InputStream inputStream = is) {
            if (inputStream == null) return;
            RefBookVersionEntity versionEntity = new RefBookVersionEntity();
            versionEntity.setId(version.getId());
            versionFileRepository.save(new VersionFileEntity(versionEntity, fileType,
                    fileStorage.saveContent(inputStream, fileNameGenerator.generateZipName(version, fileType))));
        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    private void validateDraft(Integer draftId){
        validateDraftExists(draftId);
        validateDraftNotArchived(draftId);
    }

    private void validateRefBook(Integer refBookId){
        validateRefBookExists(refBookId);
        validateRefBookNotArchived(refBookId);
    }

    private void validateRefBookNotArchived(Integer refBookId) {
        if (refBookId != null && versionRepository.exists(isVersionOfRefBook(refBookId).and(isArchived()))) {
            throw new UserException(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE);
        }
    }

    private void validateDraftNotArchived(Integer draftId) {
        if (draftId != null && versionRepository.exists(hasVersionId(draftId).and(isArchived()))) {
            throw new UserException(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE);
        }
    }

    private void validateRefBookExists(Integer refBookId) {
        if (refBookId == null || !versionRepository.exists(isVersionOfRefBook(refBookId))) {
            throw new NotFoundException(REFBOOK_NOT_FOUND_EXCEPTION_CODE);
        }
    }

    private void validateDraftExists(Integer draftId) {
        if (draftId == null || !versionRepository.exists(hasVersionId(draftId).and(isDraft()))) {
            throw new NotFoundException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, draftId));
        }
    }

    private void validateVersionExists(Integer versionId) {
        if (versionId == null || !versionRepository.exists(hasVersionId(versionId))) {
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));
        }
    }

    private void validateAttributeExists(Integer versionId, String attribute) {
        validateDraftExists(versionId);
        Structure structure = versionService.getStructure(versionId);
        if (structure.getAttribute(attribute) == null) {
            throw new NotFoundException(new Message(DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE, versionId, attribute));
        }
    }

}