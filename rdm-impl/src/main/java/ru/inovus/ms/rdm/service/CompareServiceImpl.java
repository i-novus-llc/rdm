package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria;
import ru.i_novus.platform.datastorage.temporal.service.CompareDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.CompareService;

import java.sql.Date;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static cz.atria.common.lang.Util.isEmpty;

@Service
@Primary
public class CompareServiceImpl implements CompareService {

    private CompareDataService compareDataService;
    private RefBookVersionRepository versionRepository;
    private PassportAttributeRepository passportAttributeRepository;

    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String DATA_COMPARING_UNAVAILABLE_EXCEPTION_CODE = "data.comparing.unavailable";

    @Autowired
    FieldFactory fieldFactory;

    @Autowired
    public CompareServiceImpl(CompareDataService compareDataService, RefBookVersionRepository versionRepository,
                              PassportAttributeRepository passportAttributeRepository) {
        this.compareDataService = compareDataService;
        this.versionRepository = versionRepository;
        this.passportAttributeRepository = passportAttributeRepository;
    }

    @Override
    public PassportDiff comparePassports(Integer oldVersionId, Integer newVersionId) {
        RefBookVersionEntity oldVersion = versionRepository.getOne(oldVersionId);
        RefBookVersionEntity newVersion = versionRepository.getOne(newVersionId);
        validateVersionsExistence(oldVersion, newVersion, oldVersionId, newVersionId);

        List<PassportAttributeEntity> passportAttributes = passportAttributeRepository.findAllByComparableIsTrue();
        List<PassportAttributeDiff> passportAttributeDiffList = new ArrayList<>();

        passportAttributes.forEach(passportAttribute -> {
            PassportValueEntity oldPassportValue = oldVersion.getPassportValues().stream().filter(passportValue -> passportValue.getAttribute().equals(passportAttribute)).findFirst().orElse(null);
            PassportValueEntity newPassportValue = newVersion.getPassportValues().stream().filter(passportValue -> passportValue.getAttribute().equals(passportAttribute)).findFirst().orElse(null);

            if (!equalValues(oldPassportValue, newPassportValue)) {
                PassportAttributeDiff passportAttributeDiff = new PassportAttributeDiff(
                        new PassportAttribute(passportAttribute.getCode(), passportAttribute.getName()),
                        oldPassportValue != null ? oldPassportValue.getValue() : null,
                        newPassportValue != null ? newPassportValue.getValue() : null);
                passportAttributeDiffList.add(passportAttributeDiff);
            }
        });
        return new PassportDiff(passportAttributeDiffList);
    }

    @Override
    @Transactional(readOnly = true)
    public StructureDiff compareStructures(Integer oldVersionId, Integer newVersionId) {

        RefBookVersionEntity oldVersion = versionRepository.findOne(oldVersionId);
        RefBookVersionEntity newVersion = versionRepository.findOne(newVersionId);
        if (oldVersion == null || newVersion == null)
            throw new IllegalArgumentException("invalid.version.ids");

        List<StructureDiff.AttributeDiff> inserted = new ArrayList<>();
        List<StructureDiff.AttributeDiff> updated = new ArrayList<>();
        List<StructureDiff.AttributeDiff> deleted = new ArrayList<>();

        newVersion.getStructure().getAttributes().forEach(newAttribute -> {
            Optional<Structure.Attribute> oldAttribute = oldVersion.getStructure().getAttributes().stream()
                    .filter(o -> Objects.equals(newAttribute.getCode(), o.getCode())).findAny();
            if (!oldAttribute.isPresent()) {
                inserted.add(new StructureDiff.AttributeDiff(null, newAttribute));
            } else if (oldAttribute.get().equals(newAttribute)) {
                updated.add(new StructureDiff.AttributeDiff(oldAttribute.get(), newAttribute));
            }
        });
        oldVersion.getStructure().getAttributes().stream()
                .filter(oldAttribute -> newVersion.getStructure().getAttributes().stream()
                        .noneMatch(n -> Objects.equals(oldAttribute.getCode(), n.getCode())))
                .map(oldAttribute -> new StructureDiff.AttributeDiff(oldAttribute, null))
                .forEach(deleted::add);

        return new StructureDiff(inserted, updated, deleted);
    }

    @Override
    @Transactional(readOnly = true)
    public RefBookDataDiff compareData(ru.inovus.ms.rdm.model.CompareDataCriteria criteria) {
        RefBookVersionEntity oldVersion = versionRepository.getOne(criteria.getOldVersionId());
        RefBookVersionEntity newVersion = versionRepository.getOne(criteria.getNewVersionId());
        validateVersionsExistence(oldVersion, newVersion, criteria.getOldVersionId(), criteria.getNewVersionId());

        Structure oldStructure = oldVersion.getStructure();
        Structure newStructure = newVersion.getStructure();
        validatePrimaryAttributesEquality(oldStructure.getPrimary(), newStructure.getPrimary());

        CompareDataCriteria compareDataCriteria = getCompareDataCriteria(oldVersion, newVersion);
        compareDataCriteria.setPrimaryFieldsFilters(criteria.getPrimaryFieldsFilters());
        compareDataCriteria.setPage(criteria.getPageNumber() + 1);
        compareDataCriteria.setSize(criteria.getPageSize());

        List<String> newAttributes = new ArrayList<>();
        List<String> oldAttributes = new ArrayList<>();
        List<String> updatedAttributes = new ArrayList<>();
        newStructure.getAttributes().forEach(newAttribute -> {
            Structure.Attribute old = oldStructure.getAttribute(newAttribute.getCode());
            if (old == null)
                newAttributes.add(newAttribute.getCode());
            else if (!old.storageEquals(newAttribute))
                updatedAttributes.add(newAttribute.getCode());
        });
        oldStructure.getAttributes().forEach(oldAttribute -> {
            if (newStructure.getAttribute(oldAttribute.getCode()) == null)
                oldAttributes.add(oldAttribute.getCode());
        });
        DataDifference dataDifference = compareDataService.getDataDifferenceForPeriod(compareDataCriteria);

        return new RefBookDataDiff(new DiffRowValuePage(dataDifference.getRows()), oldAttributes, newAttributes, updatedAttributes);
    }

    private CompareDataCriteria getCompareDataCriteria(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setStorageCode(oldVersion.getStorageCode());
        compareDataCriteria.setNewStorageCode(newVersion.getStorageCode());
        compareDataCriteria.setOldPublishDate(oldVersion.getFromDate() != null
                ? Date.from(oldVersion.getFromDate().atZone(ZoneId.systemDefault()).toInstant())
                : null);
        compareDataCriteria.setOldCloseDate(oldVersion.getToDate() != null
                ? Date.from(oldVersion.getToDate().atZone(ZoneId.systemDefault()).toInstant())
                : null);
        compareDataCriteria.setNewPublishDate(newVersion.getFromDate() != null
                ? Date.from(newVersion.getFromDate().atZone(ZoneId.systemDefault()).toInstant())
                : null);
        compareDataCriteria.setNewCloseDate(newVersion.getToDate() != null
                ? Date.from(newVersion.getToDate().atZone(ZoneId.systemDefault()).toInstant())
                : null);
        compareDataCriteria.setPrimaryFields(newVersion.getStructure().getPrimary()
                .stream()
                .map(Structure.Attribute::getCode)
                .collect(Collectors.toList()));
        compareDataCriteria.setFields(getCommonFields(oldVersion.getStructure(), newVersion.getStructure()));
        return compareDataCriteria;
    }

    private List<Field> getCommonFields(Structure structure1, Structure structure2) {
        return structure2.getAttributes()
                .stream()
                .filter(newAttribute -> {
                    Structure.Attribute oldAttribute = structure1.getAttribute(newAttribute.getCode());
                    return (oldAttribute != null && oldAttribute.storageEquals(newAttribute));
                })
                .map(attribute -> fieldFactory.createField(attribute.getCode(), attribute.getType()))
                .collect(Collectors.toList());
    }

    private void validateVersionsExistence(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion, Integer oldVersionId, Integer newVersionId) {
        if (oldVersion == null || newVersion == null)
            throw new UserException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, oldVersion == null ? oldVersionId : newVersionId));
    }

    private void validatePrimaryAttributesEquality(List<Structure.Attribute> oldPrimaries, List<Structure.Attribute> newPrimaries) {
        if (isEmpty(oldPrimaries)
                || isEmpty(newPrimaries)
                || oldPrimaries.size() != newPrimaries.size()
                || oldPrimaries.stream().anyMatch(oldPrimary -> newPrimaries.stream().noneMatch(newPrimary -> newPrimary.equals(oldPrimary))))
            throw new UserException(new Message(DATA_COMPARING_UNAVAILABLE_EXCEPTION_CODE));
    }

    private boolean equalValues(PassportValueEntity oldPassportValue, PassportValueEntity newPassportValue) {
        if (oldPassportValue != null && oldPassportValue.getValue() != null)
            return (newPassportValue != null && oldPassportValue.getValue().equals(newPassportValue.getValue()));
        else
            return (newPassportValue == null || newPassportValue.getValue() == null);
    }

}