package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NsiException;
import ru.inovus.ms.rdm.file.BufferedRowsPersister;
import ru.inovus.ms.rdm.file.FileProcessor;
import ru.inovus.ms.rdm.file.ProcessorFactory;
import ru.inovus.ms.rdm.file.RowsValidatorImpl;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;
import ru.inovus.ms.rdm.util.RowValuePage;
import ru.kirkazan.common.exception.CodifiedException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.MAX_TIMESTAMP;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.hasOverlappingPeriods;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isPublished;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isVersionOfRefBook;
import static ru.inovus.ms.rdm.util.ConverterUtil.fields;

@Service
public class DraftServiceImpl implements DraftService {

    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    private VersionService versionService;

    private SearchDataService searchDataService;

    private DropDataService dropDataService;

    private RefBookRepository refBookRepository;

    @Autowired
    public DraftServiceImpl(DraftDataService draftDataService, RefBookVersionRepository versionRepository, VersionService versionService,
                            RefBookRepository refBookRepository, SearchDataService searchDataService, DropDataService dropDataService) {
        this.draftDataService = draftDataService;
        this.versionRepository = versionRepository;
        this.versionService = versionService;
        this.searchDataService = searchDataService;
        this.dropDataService = dropDataService;
        this.refBookRepository = refBookRepository;
    }


    @Override
    @Transactional
    public Draft create(Integer refBookId, Structure structure) {
        RefBookVersionEntity lastRefBookVersion = getLastRefBookVersion(refBookId);
        RefBookVersionEntity draftVersion = getDraftByRefbook(refBookId);
        if (draftVersion == null && lastRefBookVersion == null) {
            throw new CodifiedException("invalid refbook");
        }
        List<Field> fields = fields(structure);
        if (draftVersion == null) {
            draftVersion = newDraftVersion(structure, lastRefBookVersion);
            draftVersion.setRefBook(refBookRepository.findOne(refBookId));
            String draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);
        } else {
            updateDraft(structure, draftVersion, fields);
        }
        RefBookVersionEntity savedDraftVersion = versionRepository.save(draftVersion);
        return new Draft(savedDraftVersion.getId(), savedDraftVersion.getStorageCode());
    }

    private RefBookVersionEntity newDraftVersion(Structure structure, RefBookVersionEntity lastRefBookVersion) {
        RefBookVersionEntity draftVersion;
        draftVersion = new RefBookVersionEntity();
        draftVersion.setStatus(RefBookVersionStatus.DRAFT);
        draftVersion.setFullName(lastRefBookVersion.getFullName());
        draftVersion.setShortName(lastRefBookVersion.getShortName());
        draftVersion.setAnnotation(lastRefBookVersion.getAnnotation());
        draftVersion.setStructure(structure);
        return draftVersion;
    }

    private void updateDraft(Structure structure, RefBookVersionEntity draftVersion, List<Field> fields) {
        String draftCode = draftVersion.getStorageCode();
        if (!structure.equals(draftVersion.getStructure())) {
            dropDataService.drop(Collections.singleton(draftCode));
            draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);
        } else {
            draftDataService.deleteAllRows(draftCode);
        }
        draftVersion.setStructure(structure);
    }

    private RefBookVersionEntity getDraftByRefbook(Integer refBookId) {
        return versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookId);
    }


    @Override
    public void updateMetadata(Integer draftId, MetadataDiff metadataDiff) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateData(Integer draftId, DataDiff dataDiff) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateData(Integer draftId, MultipartFile file) {
        RefBookVersionEntity draft = versionRepository.findOne(draftId);
        String storageCode = draft.getStorageCode();
        Structure structure = draft.getStructure();
        String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toUpperCase();
        FileProcessor validator = ProcessorFactory.createProcessor(extension,
                new RowsValidatorImpl(versionService, structure), structure, versionRepository);
        Supplier<InputStream> inputStreamSupplier = () -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new NsiException("invalid file: ", e);
            }
        };
        Result validationResult = validator.process(inputStreamSupplier);
        if (isEmpty(validationResult.getErrors())) {
            FileProcessor persister = ProcessorFactory.createProcessor(extension,
                    new BufferedRowsPersister(draftDataService, storageCode, structure), structure, versionRepository);
            persister.process(inputStreamSupplier);
        } else {
            throw new NsiException("file contains invalid reference");
        }

    }

    @Override
    public Page<RowValue> search(Integer draftId, SearchDataCriteria criteria) {
        RefBookVersionEntity draft = versionRepository.findOne(draftId);
        String storageCode = draft.getStorageCode();
        List<Field> fields = fields(draft.getStructure());
        DataCriteria dataCriteria = new DataCriteria(storageCode, null, null,
                fields, criteria.getFieldFilter(), criteria.getCommonFilter());
        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return new RowValuePage(pagedData);
    }


    @Override
    public void publish(Integer draftId, String versionName, LocalDateTime fromDate, LocalDateTime toDate) {
        RefBookVersionEntity draftVersion = versionRepository.findOne(draftId);
        validateOverlappingPeriodsInLast(fromDate, toDate, draftVersion.getRefBook().getId());
        RefBookVersionEntity lastPublishedVersion = getLastPublishedVersion(draftVersion);
        String storageCode = draftDataService.applyDraft(
                lastPublishedVersion != null ? lastPublishedVersion.getStorageCode() : null,
                draftVersion.getStorageCode(),
                Date.from(fromDate.atZone(ZoneId.systemDefault()).toInstant())
        );
        draftVersion.setStorageCode(storageCode);
        draftVersion.setVersion(versionName);
        draftVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        draftVersion.setFromDate(fromDate);
        resolveOverlappingPeriodsInFuture(fromDate, toDate, draftVersion.getRefBook().getId());
        versionRepository.save(draftVersion);
    }

    protected RefBookVersionEntity getLastPublishedVersion(RefBookVersionEntity draftVersion) {
        Page<RefBookVersionEntity> lastPublishedVersions = versionRepository
                .findAll(isPublished().and(isVersionOfRefBook(draftVersion.getRefBook().getId()))
                        , new PageRequest(1, 1, new Sort(Sort.Direction.DESC, "fromDate")));
        return lastPublishedVersions != null && lastPublishedVersions.hasContent() ? lastPublishedVersions.getContent().get(0) : null;
    }


    private void validateOverlappingPeriodsInLast(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId) {
        LocalDateTime now = LocalDateTime.now();
        if (fromDate == null || fromDate.isAfter(now)) {
            return;
        }
        if (toDate == null || toDate.isAfter(now)) {
            toDate = now;
        }

        RefBookVersionEntity refBookVersion = versionRepository.findOne(
                hasOverlappingPeriods(fromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
        );
        if (refBookVersion != null) {
            throw new UserException("overlapping.version.err");
        }

    }

    private void resolveOverlappingPeriodsInFuture(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newFromDate;
        if (toDate == null) {
            toDate = MAX_TIMESTAMP;
        }
        if (!toDate.isAfter(now)) {
            return;
        }

        if (fromDate == null || fromDate.isBefore(now)) {
            newFromDate = now;
        } else {
            newFromDate = fromDate;
        }

        Iterable<RefBookVersionEntity> versions = versionRepository.findAll(
                hasOverlappingPeriods(newFromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
        );
        if (versions != null) {
            versions.forEach(version -> {
                if (fromDate != null && fromDate.isAfter(version.getFromDate())) {
                    version.setToDate(fromDate);
                } else {
                    version.setToDate(version.getFromDate());
                }
                versionRepository.save(version);
            });
        }
    }

    private RefBookVersionEntity getLastRefBookVersion(Integer refBookId) {
        Page<RefBookVersionEntity> lastPublishedVersions = versionRepository
                .findAll(isPublished().and(isVersionOfRefBook(refBookId))
                        , new PageRequest(1, 1, new Sort(Sort.Direction.DESC, "fromDate")));
        return lastPublishedVersions != null && lastPublishedVersions.hasContent() ? lastPublishedVersions.getContent().get(0) : null;
    }

    @Override
    public void remove(Integer draftId) {
        versionRepository.delete(draftId);
    }

    @Override
    public Structure getMetadata(Integer draftId) {
        return null;
    }

    @Override
    public Draft getDraft(Integer draftId) {
        RefBookVersionEntity versionEntity = versionRepository.findOne(draftId);
        if (!versionEntity.getStatus().equals(RefBookVersionStatus.DRAFT)) {
            return null;
        }
        return versionEntity != null ? new Draft(versionEntity.getId(), versionEntity.getStorageCode()) : null;
    }

    @Override
    @Transactional
    public void createAttribute(Integer versionId, Structure.Attribute attribute, Integer referenceVersion,
                                String referenceAttribute, List<String> referenceDisplayAttributes) {
        RefBookVersionEntity draftEntity = versionRepository.findOne(versionId);
        draftDataService.addField(draftEntity.getStorageCode(), ConverterUtil.field(attribute));

        Structure structure = draftEntity.getStructure();
        if (structure == null) {
            structure = new Structure();
            structure.setAttributes(emptyList());
        }
        if (attribute.getIsPrimary())
            structure.clearPrimary();

        structure.getAttributes().add(attribute);

        if (FieldType.REFERENCE.equals(attribute.getType())) {
            if (structure.getReferences() == null)
                structure.setReferences(emptyList());
            Structure.Reference reference = buildReference(referenceVersion, attribute.getCode(),
                    referenceAttribute, referenceDisplayAttributes);
            structure.getReferences().add(reference);
        }
        draftEntity.setStructure(structure);
    }

    @Override
    @Transactional
    public void updateAttribute(Integer versionId, Structure.Attribute attribute, Integer referenceVersion,
                                String referenceAttribute, List<String> referenceDisplayAttributes) {
        RefBookVersionEntity draftEntity = versionRepository.findOne(versionId);
        draftDataService.updateField(draftEntity.getStorageCode(), ConverterUtil.field(attribute));

        Structure structure = draftEntity.getStructure();
        if (attribute.getIsPrimary())
            structure.clearPrimary();

        if (FieldType.REFERENCE.equals(attribute.getType())) {
            Integer updatableReferenceIndex = structure.getReferences().indexOf(structure.getReference(attribute.getCode()));
            Structure.Reference reference = buildReference(referenceVersion, attribute.getCode(),
                    referenceAttribute, referenceDisplayAttributes);
            structure.getReferences().set(updatableReferenceIndex, reference);
        }
        Integer updatableAttributeIndex = structure.getAttributes().indexOf(structure.getAttribute(attribute.getCode()));
        structure.getAttributes().set(updatableAttributeIndex, attribute);

    }

    @Override
    @Transactional
    public void deleteAttribute(Integer versionId, String attributeCode) {
        RefBookVersionEntity draftEntity = versionRepository.findOne(versionId);
        Structure.Attribute attribute = draftEntity.getStructure().getAttribute(attributeCode);

        if (FieldType.REFERENCE.equals(attribute.getType()))
            draftEntity.getStructure().getReferences().remove(draftEntity.getStructure().getReference(attributeCode));
        draftEntity.getStructure().getAttributes().remove(attribute);

        draftDataService.deleteField(draftEntity.getStorageCode(), attributeCode);
    }

    private Structure.Reference buildReference(Integer referenceVersion, String attributeCode,
                                               String referenceAttribute, List<String> referenceDisplayAttributes) {
        List<String> displayAttributes = isEmpty(referenceDisplayAttributes) ?
                singletonList(referenceAttribute) : referenceDisplayAttributes;
        return new Structure.Reference(attributeCode, referenceVersion, referenceAttribute, displayAttributes);
    }
}
