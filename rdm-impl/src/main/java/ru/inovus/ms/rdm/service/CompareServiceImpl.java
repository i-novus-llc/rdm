package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.enums.DiffReturnTypeEnum;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.CompareDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.ComparableField;
import ru.inovus.ms.rdm.model.compare.ComparableFieldValue;
import ru.inovus.ms.rdm.model.compare.ComparableRow;
import ru.inovus.ms.rdm.model.compare.CompareCriteria;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.sql.Date;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static cz.atria.common.lang.Util.isEmpty;
import static java.util.Collections.emptySet;
import static ru.inovus.ms.rdm.util.ComparableUtils.*;
import static ru.inovus.ms.rdm.util.ConverterUtil.getFieldSearchCriteriaList;

@Service
@Primary
public class CompareServiceImpl implements CompareService {

    private CompareDataService compareDataService;
    private VersionService versionService;
    private RefBookVersionRepository versionRepository;
    private PassportAttributeRepository passportAttributeRepository;
    private FieldFactory fieldFactory;

    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String DATA_COMPARING_UNAVAILABLE_EXCEPTION_CODE = "data.comparing.unavailable";

    @Autowired
    public CompareServiceImpl(CompareDataService compareDataService,
                              VersionService versionService, RefBookVersionRepository versionRepository,
                              PassportAttributeRepository passportAttributeRepository, FieldFactory fieldFactory) {
        this.compareDataService = compareDataService;
        this.versionService = versionService;
        this.versionRepository = versionRepository;
        this.passportAttributeRepository = passportAttributeRepository;
        this.fieldFactory = fieldFactory;
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
    public RefBookDataDiff compareData(ru.inovus.ms.rdm.model.compare.CompareDataCriteria criteria) {
        RefBookVersionEntity oldVersion = versionRepository.getOne(criteria.getOldVersionId());
        RefBookVersionEntity newVersion = versionRepository.getOne(criteria.getNewVersionId());
        validateVersionsExistence(oldVersion, newVersion, criteria.getOldVersionId(), criteria.getNewVersionId());

        Structure oldStructure = oldVersion.getStructure();
        Structure newStructure = newVersion.getStructure();
        validatePrimaryAttributesEquality(oldStructure.getPrimary(), newStructure.getPrimary());

        CompareDataCriteria compareDataCriteria = createCompareDataCriteria(oldVersion, newVersion, criteria);

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
        DataDifference dataDifference = compareDataService.getDataDifference(compareDataCriteria);

        return new RefBookDataDiff(new DiffRowValuePage(dataDifference.getRows()), oldAttributes, newAttributes, updatedAttributes);
    }

    @Override
    public Page<ComparableRow> getCommonComparableRows(CompareCriteria criteria) {
        Structure newStructure = versionService.getStructure(criteria.getNewVersionId());
        Structure oldStructure = versionService.getStructure(criteria.getOldVersionId());

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), null);
        Page<RowValue> newData = versionService.search(criteria.getNewVersionId(), searchDataCriteria);

        RefBookDataDiff refBookDataDiff = getRefBookDataDiff(criteria, newData, newStructure);

        List<ComparableField> comparableFields = createCommonComparableFieldsList(refBookDataDiff, newStructure, oldStructure);
        List<ComparableRow> comparableRows = new ArrayList<>();

        addNewVersionRows(comparableRows, comparableFields, newData, refBookDataDiff, newStructure, criteria);
        addDeletedRows(comparableRows, comparableFields, criteria, (int) newData.getTotalElements());

        return new RestPage<>(comparableRows, criteria, newData.getTotalElements() + getTotalDeletedCount(criteria));
    }

    private CompareDataCriteria createCompareDataCriteria(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion,
                                                          ru.inovus.ms.rdm.model.compare.CompareDataCriteria rdmCriteria) {
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

        compareDataCriteria.setPrimaryFieldsFilters(getFieldSearchCriteriaList(rdmCriteria.getPrimaryAttributesFilters()));
        compareDataCriteria.setCountOnly(rdmCriteria.getCountOnly() != null ? rdmCriteria.getCountOnly() : false);
        compareDataCriteria.setReturnType(getDiffReturnType(rdmCriteria.getDiffStatus()));
        compareDataCriteria.setPage(rdmCriteria.getPageNumber() + 1);
        compareDataCriteria.setSize(rdmCriteria.getPageSize());
        return compareDataCriteria;
    }

    private DiffReturnTypeEnum getDiffReturnType(DiffStatusEnum status) {
        if (status == null)
            return DiffReturnTypeEnum.ALL;
        if (DiffStatusEnum.DELETED.equals(status))
            return DiffReturnTypeEnum.OLD;
        if (DiffStatusEnum.INSERTED.equals(status))
            return DiffReturnTypeEnum.NEW;
        return null;
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

    private RefBookDataDiff getRefBookDataDiff(CompareCriteria criteria, Page<RowValue> data, Structure structure) {
        ru.inovus.ms.rdm.model.compare.CompareDataCriteria compareDataCriteria = new ru.inovus.ms.rdm.model.compare.CompareDataCriteria(criteria);
        compareDataCriteria.setPrimaryAttributesFilters(createPrimaryAttributesFilters(data, structure));

        return compareData(compareDataCriteria);
    }

    private long getTotalDeletedCount(CompareCriteria criteria) {
        ru.inovus.ms.rdm.model.compare.CompareDataCriteria deletedCountCriteria = new ru.inovus.ms.rdm.model.compare.CompareDataCriteria(criteria);
        deletedCountCriteria.setDiffStatus(DiffStatusEnum.DELETED);
        deletedCountCriteria.setPrimaryAttributesFilters(emptySet());
        deletedCountCriteria.setCountOnly(true);
        RefBookDataDiff refBookDeletedRows = compareData(deletedCountCriteria);
        return refBookDeletedRows.getRows().getTotalElements();
    }

    private void addNewVersionRows(List<ComparableRow> comparableRows, List<ComparableField> comparableFields,
                                   Page<RowValue> newData, RefBookDataDiff refBookDataDiff,
                                   Structure newStructure, CompareCriteria criteria) {
        if (CollectionUtils.isEmpty(newData.getContent()))
            return;

        Boolean hasUpdOrDelAttr = !CollectionUtils.isEmpty(refBookDataDiff.getUpdatedAttributes()) || !CollectionUtils.isEmpty(refBookDataDiff.getOldAttributes());

        SearchDataCriteria oldSearchDataCriteria = hasUpdOrDelAttr
                ? new SearchDataCriteria(0, criteria.getPageSize(), createPrimaryAttributesFilters(newData, newStructure))
                : null;

        Page<RowValue> oldData = hasUpdOrDelAttr
                ? versionService.search(criteria.getOldVersionId(), oldSearchDataCriteria)
                : null;

        newData.getContent()
                .forEach(newRowValue -> {
                    ComparableRow comparableRow = new ComparableRow();
                    DiffRowValue diffRowValue = getDiffRowValue(newStructure.getPrimary(), newRowValue,
                            refBookDataDiff.getRows().getContent());
                    RowValue oldRowValue = oldData != null
                            ? findRowValue(newStructure.getPrimary(), newRowValue, oldData.getContent())
                            : null;

                    comparableRow.setStatus(diffRowValue != null ? diffRowValue.getStatus() : null);
                    comparableRow.setFieldValues(
                            comparableFields
                                    .stream()
                                    .map(comparableField ->
                                            new ComparableFieldValue(comparableField,
                                                    diffRowValue != null
                                                            ? diffRowValue.getDiffFieldValue(comparableField.getCode())
                                                            : null,
                                                    oldRowValue,
                                                    newRowValue))
                                    .collect(Collectors.toList())
                    );
                    comparableRows.add(comparableRow);
                });
    }

    private void addDeletedRows(List<ComparableRow> comparableRows, List<ComparableField> comparableFields,
                                CompareCriteria criteria, int totalNewCount) {
        if (comparableRows.size() < criteria.getPageSize()) {
            int skipPageCount = criteria.getPageNumber() - totalNewCount / criteria.getPageSize();
            long newDataOnLastPageCount = totalNewCount % criteria.getPageSize();
            long skipDeletedRowsCount = criteria.getPageSize() * skipPageCount - newDataOnLastPageCount;
            long pageSize = skipDeletedRowsCount + criteria.getPageSize();
            SearchDataCriteria delSearchDataCriteria = new SearchDataCriteria(0, (int) pageSize, null);
            Page<RowValue> delData = versionService.search(criteria.getOldVersionId(), delSearchDataCriteria);
            delData.getContent()
                    .stream()
                    .skip(skipDeletedRowsCount > 0 ? skipDeletedRowsCount : 0)
                    .forEach(deletedRowValue -> {
                        ComparableRow comparableRow = new ComparableRow();
                        comparableRow.setStatus(DiffStatusEnum.DELETED);
                        comparableRow.setFieldValues(
                                comparableFields
                                        .stream()
                                        .map(comparableField ->
                                                new ComparableFieldValue(comparableField,
                                                        null,
                                                        deletedRowValue,
                                                        null))
                                        .collect(Collectors.toList())
                        );
                        comparableRows.add(comparableRow);
                    });
        }
    }

}