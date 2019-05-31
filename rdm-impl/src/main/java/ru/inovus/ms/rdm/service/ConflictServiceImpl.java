package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.*;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.RowUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.util.ComparableUtils.findRefBookRowValue;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private CompareService compareService;

    private VersionService versionService;

    private DraftService draftService;

    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    private static final String VERSION_NOT_FOUND = "version.not.found";
    private static final String CONFLICTED_ROW_NOT_FOUND = "conflicted.row.not.found";

    @Autowired
    public ConflictServiceImpl(CompareService compareService,
                               VersionService versionService,
                               DraftService draftService,
                               DraftDataService draftDataService,
                               RefBookVersionRepository versionRepository) {
        this.compareService = compareService;
        this.versionService = versionService;
        this.draftService = draftService;
        this.draftDataService = draftDataService;

        this.versionRepository = versionRepository;
    }

    @Override
    public List<Conflict> calculateConflicts(Integer refFromId, Integer refToId) {
        validateVersionsExistence(refFromId);
        validateVersionsExistence(refToId);

        RefBookVersion refFromVersion = versionService.getById(refFromId);
        RefBookVersion refToVersion = versionService.getById(refToId);
        RefBookVersionEntity refToDraftVersion = versionRepository
                .findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refToVersion.getRefBookId());

//        на данный момент может быть только: 1 поле -> 1 первичный ключ (ссылка на составной ключ невозможна)
        List<Structure.Attribute> refAttributes = refFromVersion.getStructure()
                .getRefCodeReferences(refToVersion.getCode())
                .stream()
                .map(ref ->
                        ref.findReferenceAttribute(refFromVersion.getStructure()))
                .collect(toList());

        Page<RefBookRowValue> refFromRowValues = versionService.search(refFromId, new SearchDataCriteria());

        RefBookDataDiff dataDiff = compareService.compareData(new CompareDataCriteria(refToId, refToDraftVersion.getId()));

        return createConflicts(dataDiff.getRows().getContent(), refFromRowValues.getContent(),
                refToVersion.getStructure(), refFromVersion.getStructure(), refAttributes);
    }

    private List<Conflict> createConflicts(List<DiffRowValue> diffRowValues, List<RefBookRowValue> refFromRowValues,
                                           Structure refToStructure, Structure refFromStructure,
                                           List<Structure.Attribute> refFromAttributes) {
        return diffRowValues
                .stream()
                .filter(diffRowValue ->
                        asList(DiffStatusEnum.DELETED, DiffStatusEnum.UPDATED)
                                .contains(diffRowValue.getStatus()))
                .map(diffRowValue -> {
                    RefBookRowValue refFromRowValue = findRefBookRowValue(refToStructure.getPrimary(), refFromAttributes,
                            diffRowValue, refFromRowValues);
                    if (refFromRowValue == null)
                        return null;

                    return createConflict(diffRowValue, refFromRowValue, refFromStructure);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private Conflict createConflict(DiffRowValue diffRowValue, RefBookRowValue refFromRowValue, Structure refFromStructure) {
        Conflict conflict = new Conflict();
        conflict.setConflictType(
                diffRowValue.getStatus().equals(DiffStatusEnum.DELETED)
                        ? ConflictType.DELETED
                        : ConflictType.UPDATED);
        conflict.setPrimaryValues(convertToFieldValues(refFromRowValue, refFromStructure));
        return conflict;
    }

    private List<FieldValue> convertToFieldValues(RefBookRowValue refFromRowValue, Structure refFromStructure) {
        return refFromRowValue
                .getFieldValues()
                .stream()
                .filter(fieldValue ->
                        refFromStructure.getAttribute(fieldValue.getField()).getIsPrimary())
                .collect(toList());
    }

    /**
     * Получение конфликтной записи по конфликту.
     */
    private RefBookRowValue getRefToRowValue(RefBookVersion version, Conflict conflict) {

        if (version == null || conflict == null ||
                CollectionUtils.isEmpty(conflict.getPrimaryValues()))
            return null;

        // Convert conflict to criteria.
        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        conflict.getPrimaryValues().forEach(fieldValue -> {
            FieldType fieldType = version.getStructure().getAttribute(fieldValue.getField()).getType();
            filters.add(new AttributeFilter(fieldValue.getField(), fieldValue.getValue(), fieldType, SearchTypeEnum.EXACT));
        });
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(version.getId(), criteria);
        return (rowValues != null && !rowValues.isEmpty()) ? rowValues.get().findFirst().orElse(null) : null;
    }

    /**
     * Получение записей со ссылками на конфликтную запись по конфликту.
     */
    private Page<RefBookRowValue> getRefFromRowValues(RefBookVersion version, Conflict conflict,
                                                      Structure.Reference reference, String referenceValue) {

        if (version == null || conflict == null ||
                CollectionUtils.isEmpty(conflict.getPrimaryValues()))
            return null;

        // Convert conflict to criteria.
        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        AttributeFilter filter = new AttributeFilter(reference.getAttribute(), referenceValue, FieldType.REFERENCE, SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(version.getId(), criteria);
        return (rowValues != null && !rowValues.isEmpty()) ? rowValues : null;
    }

    /**
     * Обновление ссылки в справочнике.
     *
     * @param refFromId          идентификатор версии справочника
     * @param refFromStorageCode код хранилища версии справочника
     * @param rowSystemId        системный идентификатор записи
     * @param referenceFieldName название поля-ссылки
     * @param fieldReference     данные для обновления
     */
    private void updateReferenceValue(Integer refFromId, String refFromStorageCode, Long rowSystemId,
                                      String referenceFieldName, Reference fieldReference) {
        FieldValue fieldValue = new ReferenceFieldValue(referenceFieldName, fieldReference);
        LongRowValue rowValue = new LongRowValue(rowSystemId, singletonList(fieldValue));

        draftDataService.updateRow(refFromStorageCode, new RefBookRowValue(rowValue, refFromId));
    }

    /**
     * Обновление ссылок в справочнике по конфликту.
     *
     * @param refFromDraftVersion версия справочника со ссылками
     * @param refToVersion        версия изменённого справочника
     * @param conflict            конфликт
     * @param refFromStorageCode  код хранилища справочника со ссылками
     * @param refToStorageCode    код хранилища изменённого справочника
     */
    private void updateReferenceValues(RefBookVersion refFromDraftVersion,
                                       RefBookVersion refToVersion,
                                       Conflict conflict,
                                       String refFromStorageCode,
                                       String refToStorageCode) {
        if (conflict == null ||
                CollectionUtils.isEmpty(conflict.getPrimaryValues()))
            return;

        RefBookRowValue refToRow = getRefToRowValue(refToVersion, conflict);
        if (refToRow == null)
            throw new RdmException(CONFLICTED_ROW_NOT_FOUND);

        String primaryName = conflict.getPrimaryValues().get(0).getField();
        String primaryValue = conflict.getPrimaryValues().get(0).getValue().toString();

        List<Structure.Reference> references = refFromDraftVersion.getStructure().getRefCodeReferences(refToVersion.getCode());
        references.forEach(reference -> {
            Page<RefBookRowValue> refFromRows = getRefFromRowValues(refFromDraftVersion, conflict, reference, primaryValue);
            if (refFromRows == null || refFromRows.isEmpty())
                return;

            refFromRows.forEach(refBookRowValue -> {
                FieldValue fieldValue = refBookRowValue.getFieldValue(reference.getAttribute());
                if (fieldValue instanceof ReferenceFieldValue) {
                    Reference oldReference = ((ReferenceFieldValue) fieldValue).getValue();
                    String displayValue = RowUtils.toDisplayValue(reference.getDisplayExpression(), refToRow);

                    if (!Objects.equals(oldReference.getDisplayValue(), displayValue)) {
                        Reference newReference = new Reference(
                                refToStorageCode,
                                null, // SYS_PUBLISH_TIME is not exist for publishing draft
                                primaryName, // referenceAttribute
                                new DisplayExpression(reference.getDisplayExpression()),
                                oldReference.getValue(),
                                displayValue);

                        updateReferenceValue(refFromDraftVersion.getId(),
                                refFromStorageCode,
                                refBookRowValue.getSystemId(),
                                reference.getAttribute(),
                                newReference);
                    }
                }
            });
        });
    }

    /**
     * Обновление ссылок в справочнике по списку конфликтов.
     *
     * @param refFromId идентификатор версии справочника со ссылками
     * @param refToId   идентификатор версии изменённого справочника
     * @param conflicts список конфликтов
     */
    @Override
    public void updateReferenceValues(Integer refFromId, Integer refToId, List<Conflict> conflicts) {

        if (CollectionUtils.isEmpty(conflicts))
            return;

        validateVersionsExistence(refFromId);
        validateVersionsExistence(refToId);

        RefBookVersion refFromVersion = versionService.getById(refFromId);
        RefBookVersion refToVersion = versionService.getById(refToId);

        Draft refFromDraft;
        if (RefBookVersionStatus.DRAFT.equals(refFromVersion.getStatus()))
            refFromDraft = draftService.getDraft(refFromId);
        else
            refFromDraft = draftService.createFromVersion(refFromId);
        RefBookVersion refFromDraftVersion = versionService.getById(refFromDraft.getId());
        Draft refToDraft = draftService.getDraft(refToId);

        conflicts.stream()
                .filter(conflict -> ConflictType.UPDATED.equals(conflict.getConflictType()))
                .forEach(conflict -> updateReferenceValues(refFromDraftVersion, refToVersion, conflict,
                        refFromDraft.getStorageCode(),
                        refToDraft.getStorageCode()));
    }

    private void validateVersionsExistence(Integer versionId) {
        if (versionId == null || !versionRepository.existsById(versionId))
            throw new NotFoundException(new Message(VERSION_NOT_FOUND, versionId));
    }
}
