package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.kirkazan.common.exception.CodifiedException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;
import static ru.inovus.ms.rdm.util.ConverterUtil.attributeToField;
import static ru.inovus.ms.rdm.util.ConverterUtil.structureToFields;

@Service
public class DraftServiceImpl implements DraftService {

    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    private FieldFactory fieldFactory;

    private SearchDataService searchDataService;

    private DropDataService dropDataService;

    private RefBookRepository refBookRepository;

    private static final String ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE = "Невозможно обновить атрибут";

    @Autowired
    public DraftServiceImpl(DraftDataService draftDataService, RefBookVersionRepository versionRepository, FieldFactory fieldFactory,
                            RefBookRepository refBookRepository, SearchDataService searchDataService, DropDataService dropDataService) {
        this.draftDataService = draftDataService;
        this.versionRepository = versionRepository;
        this.fieldFactory = fieldFactory;
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
        List<Field> fields = structureToFields(structure, fieldFactory);
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
    public void updateData(Integer draftId, FileData file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<RowValue> search(Integer draftId, SearchDataCriteria criteria) {
        RefBookVersionEntity draft = versionRepository.findOne(draftId);
        String storageCode = draft.getStorageCode();
        List<Field> fields = structureToFields(draft.getStructure(), fieldFactory);
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
        return versionEntity != null ? new Draft(versionEntity.getId(), versionEntity.getStorageCode()) : null;
    }

    @Override
    @Transactional
    public void createAttribute(CreateAttribute createAttribute) {

        RefBookVersionEntity draftEntity = versionRepository.findOne(createAttribute.getVersionId());
        Structure.Attribute attribute = createAttribute.getAttribute();
        Structure.Reference reference = createAttribute.getReference();
        draftDataService.addField(draftEntity.getStorageCode(), attributeToField(attribute, fieldFactory));

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
            structure.getReferences().add(reference);
        }
        draftEntity.setStructure(structure);
    }

    @Override
    @Transactional
    public void updateAttribute(UpdateAttribute updateAttribute) {
        RefBookVersionEntity draftEntity = versionRepository.findOne(updateAttribute.getVersionId());
        Structure structure = draftEntity.getStructure();
        Structure.Attribute attribute = structure.getAttribute(updateAttribute.getCode());
        validateUpdateAttribute(updateAttribute, attribute);

        //clear previous primary keys
        if (updateAttribute.getIsPrimary() != null
                && updateAttribute.getIsPrimary().isPresent()
                && updateAttribute.getIsPrimary().get())
            structure.clearPrimary();

        setValueIfPresent(updateAttribute::getName, attribute::setName);
        setValueIfPresent(updateAttribute::getDescription, attribute::setDescription);
        setValueIfPresent(updateAttribute::getIsRequired, attribute::setIsRequired);
        setValueIfPresent(updateAttribute::getIsPrimary, attribute::setPrimary);

        draftDataService.updateField(draftEntity.getStorageCode(), attributeToField(attribute, fieldFactory));

        if (FieldType.REFERENCE.equals(updateAttribute.getType())) {
            Structure.Reference reference;
            if (FieldType.REFERENCE.equals(attribute.getType())) {
                reference = structure.getReference(updateAttribute.getCode());
            } else {
                reference = new Structure.Reference();
            }
            Integer updatableReferenceIndex = structure.getReferences().indexOf(reference);
            updateReference(updateAttribute, reference);
            if (updatableReferenceIndex >= 0)
                structure.getReferences().set(updatableReferenceIndex, reference);
            else
                structure.getReferences().add(reference);
        } else if (FieldType.REFERENCE.equals(attribute.getType())) {
            structure.getReferences().remove(structure.getReference(updateAttribute.getCode()));
        }
        attribute.setType(updateAttribute.getType());
    }

    private void updateReference(UpdateAttribute updateAttribute, Structure.Reference updatableReference) {
        setValueIfPresent(updateAttribute::getAttribute, updatableReference::setAttribute);
        setValueIfPresent(updateAttribute::getReferenceVersion, updatableReference::setReferenceVersion);
        setValueIfPresent(updateAttribute::getReferenceAttribute, updatableReference::setReferenceAttribute);
        setValueIfPresent(updateAttribute::getDisplayAttributes, updatableReference::setDisplayAttributes);
        setValueIfPresent(updateAttribute::getSortingAttributes, updatableReference::setSortingAttributes);
    }

    private <T> void setValueIfPresent(Supplier<UpdateValue<T>> updAttrValueGetter, Consumer<T> attrValueSetter) {
        UpdateValue<T> value = updAttrValueGetter.get();
        if (value != null) {
            if (value.isPresent()) {
                attrValueSetter.accept(value.get());
            } else {
                attrValueSetter.accept(null);
            }
        }
    }

    private void validateUpdateAttribute(UpdateAttribute updateAttribute, Structure.Attribute attribute) {
        if (attribute == null
                || updateAttribute.getVersionId() == null
                || updateAttribute.getType() == null)
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);
        if (FieldType.REFERENCE.equals(updateAttribute.getType()) &&
                (FieldType.REFERENCE.equals(attribute.getType()) && validateReferenceValues(updateAttribute, this::isUpdateValueNotNullAndEmpty)
                || (!FieldType.REFERENCE.equals(attribute.getType()) && validateReferenceValues(updateAttribute, this::isUpdateValueNullOrEmpty))))
            throw new IllegalArgumentException(ILLEGAL_UPDATE_ATTRIBUTE_EXCEPTION_CODE);
    }

    private boolean validateReferenceValues(UpdateAttribute updateAttribute, Function<UpdateValue, Boolean> valueValidateFunc) {
        return valueValidateFunc.apply(updateAttribute.getReferenceVersion())
                || valueValidateFunc.apply(updateAttribute.getAttribute())
                || valueValidateFunc.apply(updateAttribute.getReferenceAttribute());
    }

    private boolean isUpdateValueNotNullAndEmpty(UpdateValue updateValue) {
        return updateValue != null && !updateValue.isPresent();
    }

    private boolean isUpdateValueNullOrEmpty(UpdateValue updateValue) {
        return updateValue == null || !updateValue.isPresent();
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

}
