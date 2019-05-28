package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.util.ComparableUtils.findRefBookRowValue;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private VersionService versionService;

    private CompareService compareService;

    private RefBookVersionRepository versionRepository;

    @Autowired
    public ConflictServiceImpl(VersionService versionService, CompareService compareService,
                               RefBookVersionRepository versionRepository) {
        this.versionService = versionService;
        this.compareService = compareService;
        this.versionRepository = versionRepository;
    }

    @Override
    public List<Conflict> calculateConflicts(Integer refFromId, Integer refToId) {

        RefBookVersion refFromVersion = versionService.getById(refFromId);
        RefBookVersion refToVersion = versionService.getById(refToId);
        RefBookVersionEntity refToDraftVersion = versionRepository
                .findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refToVersion.getRefBookId());

//        на данный момент может быть только: 1 поле -> 1 первичный ключ (ссылка на составной ключ невозможна)
        List<Structure.Attribute> refAttributes = refFromVersion.getStructure()
                .getReferences()
                .stream()
                .filter(ref ->
                        refToVersion.getCode().equals(ref.getReferenceCode()))
                .map(ref ->
                        refFromVersion.getStructure().getAttribute(ref.getAttribute()))
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

}
