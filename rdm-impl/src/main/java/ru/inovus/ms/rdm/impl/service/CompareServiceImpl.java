package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.CompareDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.api.service.CompareService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.impl.entity.PassportValueEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.compare.ComparableField;
import ru.inovus.ms.rdm.api.model.compare.ComparableFieldValue;
import ru.inovus.ms.rdm.api.model.compare.ComparableRow;
import ru.inovus.ms.rdm.api.model.compare.CompareCriteria;
import ru.inovus.ms.rdm.api.model.diff.DiffRowValuePage;
import ru.inovus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.inovus.ms.rdm.api.model.diff.StructureDiff;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.PassportAttribute;
import ru.inovus.ms.rdm.api.model.diff.PassportAttributeDiff;
import ru.inovus.ms.rdm.api.model.diff.PassportDiff;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.impl.util.ConverterUtil;
import ru.inovus.ms.rdm.impl.repository.PassportAttributeRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.api.validation.VersionValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.api.util.ComparableUtils.*;
import static ru.inovus.ms.rdm.impl.util.ConverterUtil.getFieldSearchCriteriaList;

@Service
@Primary
public class CompareServiceImpl implements CompareService {

    private static final String COMPARE_OLD_PRIMARIES_NOT_FOUND_EXCEPTION_CODE = "compare.old.primaries.not.found";
    private static final String COMPARE_NEW_PRIMARIES_NOT_FOUND_EXCEPTION_CODE = "compare.new.primaries.not.found";
    private static final String COMPARE_PRIMARIES_NOT_EQUALS_EXCEPTION_CODE = "compare.primaries.not.equals";

    private CompareDataService compareDataService;
    private VersionService versionService;
    private RefBookVersionRepository versionRepository;
    private PassportAttributeRepository passportAttributeRepository;
    private FieldFactory fieldFactory;
    private VersionValidation versionValidation;

    @Autowired
    public CompareServiceImpl(CompareDataService compareDataService,
                              VersionService versionService, RefBookVersionRepository versionRepository,
                              PassportAttributeRepository passportAttributeRepository, FieldFactory fieldFactory,
                              VersionValidation versionValidation) {
        this.compareDataService = compareDataService;
        this.versionService = versionService;
        this.versionRepository = versionRepository;
        this.passportAttributeRepository = passportAttributeRepository;
        this.fieldFactory = fieldFactory;
        this.versionValidation = versionValidation;
    }

    @Override
    @Transactional(readOnly = true)
    public PassportDiff comparePassports(Integer oldVersionId, Integer newVersionId) {

        validateVersionPairExists(oldVersionId, newVersionId);

        RefBookVersionEntity oldVersion = versionRepository.getOne(oldVersionId);
        RefBookVersionEntity newVersion = versionRepository.getOne(newVersionId);

        List<PassportAttributeEntity> passportAttributes = passportAttributeRepository.findAllByComparableIsTrueOrderByPositionAsc();
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
    public StructureDiff compareStructures(Structure oldStructure, Structure newStructure) {

        List<StructureDiff.AttributeDiff> inserted = new ArrayList<>();
        List<StructureDiff.AttributeDiff> updated = new ArrayList<>();
        List<StructureDiff.AttributeDiff> deleted = new ArrayList<>();

        newStructure.getAttributes().forEach(newAttribute -> {
            Structure.Attribute oldAttribute = oldStructure.getAttribute(newAttribute.getCode());
            if (oldAttribute == null)
                inserted.add(new StructureDiff.AttributeDiff(null, newAttribute));
            else if (!oldAttribute.equals(newAttribute))
                updated.add(new StructureDiff.AttributeDiff(oldAttribute, newAttribute));
        });

        oldStructure.getAttributes().forEach(oldAttribute -> {
            Structure.Attribute newAttribute = newStructure.getAttribute(oldAttribute.getCode());
            if (newAttribute == null)
                deleted.add(new StructureDiff.AttributeDiff(oldAttribute, null));
        });

        return new StructureDiff(inserted, updated, deleted);
    }

    @Override
    @Transactional(readOnly = true)
    public StructureDiff compareStructures(Integer oldVersionId, Integer newVersionId) {

        validateVersionPairExists(oldVersionId, newVersionId);

        RefBookVersionEntity oldVersion = versionRepository.getOne(oldVersionId);
        RefBookVersionEntity newVersion = versionRepository.getOne(newVersionId);

        Structure oldStructure = oldVersion.getStructure();
        Structure newStructure = newVersion.getStructure();

        return compareStructures(oldStructure, newStructure);
    }

    @Override
    @Transactional(readOnly = true)
    public RefBookDataDiff compareData(ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria criteria) {

        validateVersionPairExists(criteria.getOldVersionId(), criteria.getNewVersionId());

        RefBookVersionEntity oldVersion = versionRepository.getOne(criteria.getOldVersionId());
        RefBookVersionEntity newVersion = versionRepository.getOne(criteria.getNewVersionId());

        Structure oldStructure = oldVersion.getStructure();
        Structure newStructure = newVersion.getStructure();
        validatePrimariesEquality(oldStructure.getPrimary(), newStructure.getPrimary());

        CompareDataCriteria compareDataCriteria = createVdsCompareDataCriteria(oldVersion, newVersion, criteria);

        List<String> newAttributes = new ArrayList<>();
        List<String> oldAttributes = new ArrayList<>();
        List<String> updatedAttributes = new ArrayList<>();

        newStructure.getAttributes().forEach(newAttribute -> {
            Structure.Attribute oldAttribute = oldStructure.getAttribute(newAttribute.getCode());
            if (oldAttribute == null)
                newAttributes.add(newAttribute.getCode());
            else if (!oldAttribute.storageEquals(newAttribute))
                updatedAttributes.add(newAttribute.getCode());
        });

        oldStructure.getAttributes().forEach(oldAttribute -> {
            Structure.Attribute newAttribute = newStructure.getAttribute(oldAttribute.getCode());
            if (newAttribute == null)
                oldAttributes.add(oldAttribute.getCode());
        });

        DataDifference dataDifference = compareDataService.getDataDifference(compareDataCriteria);

        return new RefBookDataDiff(new DiffRowValuePage(dataDifference.getRows()), oldAttributes, newAttributes, updatedAttributes);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComparableRow> getCommonComparableRows(ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria criteria) {

        validateVersionPairExists(criteria.getNewVersionId(), criteria.getOldVersionId());

        Structure newStructure = versionService.getStructure(criteria.getNewVersionId());
        Structure oldStructure = versionService.getStructure(criteria.getOldVersionId());

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), criteria.getPrimaryAttributesFilters());
        Page<RefBookRowValue> newData = versionService.search(criteria.getNewVersionId(), searchDataCriteria);

        RefBookDataDiff refBookDataDiff = compareData(createVdsCompareDataCriteria(criteria, newData, newStructure));
        RefBookDataDiff refBookDataDiffDeleted = compareData(createRdmCompareDataCriteriaDeleted(criteria));

        List<ComparableField> comparableFields = createCommonComparableFieldsList(refBookDataDiff, newStructure, oldStructure);
        List<ComparableRow> comparableRows = new ArrayList<>();

        addNewVersionRows(comparableRows, comparableFields, newData, refBookDataDiff, newStructure, criteria);
        addDeletedRows(comparableRows, comparableFields, refBookDataDiffDeleted, oldStructure, criteria, (int) newData.getTotalElements());

        return new RestPage<>(comparableRows, criteria, newData.getTotalElements() + refBookDataDiffDeleted.getRows().getTotalElements());
    }

    private List<Field> getCommonFields(Structure structure1, Structure structure2) {
        return structure2.getAttributes()
                .stream()
                .filter(newAttribute -> {
                    Structure.Attribute oldAttribute = structure1.getAttribute(newAttribute.getCode());
                    return (oldAttribute != null && oldAttribute.storageEquals(newAttribute));
                })
                .map(attribute -> fieldFactory.createField(attribute.getCode(), attribute.getType()))
                .collect(toList());
    }

    private boolean equalValues(PassportValueEntity oldPassportValue, PassportValueEntity newPassportValue) {
        if (oldPassportValue != null && oldPassportValue.getValue() != null)
            return (newPassportValue != null && oldPassportValue.getValue().equals(newPassportValue.getValue()));
        else
            return (newPassportValue == null || newPassportValue.getValue() == null);
    }

    private CompareDataCriteria createVdsCompareDataCriteria(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion,
                                                             ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria rdmCriteria) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setStorageCode(oldVersion.getStorageCode());
        compareDataCriteria.setNewStorageCode(newVersion.getStorageCode());
        compareDataCriteria.setOldPublishDate(oldVersion.getFromDate());
        compareDataCriteria.setOldCloseDate(oldVersion.getToDate());
        compareDataCriteria.setNewPublishDate(newVersion.getFromDate());
        compareDataCriteria.setNewCloseDate(newVersion.getToDate());
        compareDataCriteria.setPrimaryFields(newVersion.getStructure().getPrimary()
                .stream()
                .map(Structure.Attribute::getCode)
                .collect(Collectors.toList()));
        compareDataCriteria.setFields(getCommonFields(oldVersion.getStructure(), newVersion.getStructure()));

        compareDataCriteria.setPrimaryFieldsFilters(ConverterUtil.getFieldSearchCriteriaList(rdmCriteria.getPrimaryAttributesFilters()));
        compareDataCriteria.setCountOnly(rdmCriteria.getCountOnly() != null && rdmCriteria.getCountOnly());
        compareDataCriteria.setStatus(rdmCriteria.getDiffStatus());
        compareDataCriteria.setPage(rdmCriteria.getPageNumber() + 1);
        compareDataCriteria.setSize(rdmCriteria.getPageSize());
        return compareDataCriteria;
    }

    private ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria createVdsCompareDataCriteria(CompareCriteria criteria, Page<? extends RowValue> data, Structure structure) {
        ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria rdmCriteria = new ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria(criteria);
        rdmCriteria.setPrimaryAttributesFilters(createPrimaryAttributesFilters(data, structure));
        return rdmCriteria;
    }

    private ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria createRdmCompareDataCriteriaDeleted(CompareCriteria criteria) {
        ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria deletedRdmCriteria = new ru.inovus.ms.rdm.api.model.compare.CompareDataCriteria(criteria);
        deletedRdmCriteria.setPrimaryAttributesFilters(emptySet());
        deletedRdmCriteria.setDiffStatus(DiffStatusEnum.DELETED);
        deletedRdmCriteria.setCountOnly(false);
        return deletedRdmCriteria;
    }

    private void addNewVersionRows(List<ComparableRow> comparableRows, List<ComparableField> comparableFields,
                                   Page<? extends RowValue> newData, RefBookDataDiff refBookDataDiff,
                                   Structure newStructure, CompareCriteria criteria) {
        if (isEmpty(newData.getContent()))
            return;

        boolean hasUpdOrDelAttr = !isEmpty(refBookDataDiff.getUpdatedAttributes()) || !isEmpty(refBookDataDiff.getOldAttributes());

        SearchDataCriteria oldSearchDataCriteria = hasUpdOrDelAttr
                ? new SearchDataCriteria(0, criteria.getPageSize(), createPrimaryAttributesFilters(newData, newStructure))
                : null;

        Page<RefBookRowValue> oldData = hasUpdOrDelAttr
                ? versionService.search(criteria.getOldVersionId(), oldSearchDataCriteria)
                : null;

        newData.getContent()
                .forEach(newRowValue -> {
                    ComparableRow comparableRow = new ComparableRow();
                    DiffRowValue diffRowValue = findDiffRowValue(newStructure.getPrimary(), newRowValue,
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
                                                    newRowValue,
                                                    diffRowValue != null ? diffRowValue.getStatus() : null))
                                    .collect(toList())
                    );
                    comparableRows.add(comparableRow);
                });
    }

    private void addDeletedRows(List<ComparableRow> comparableRows, List<ComparableField> comparableFields,
                                RefBookDataDiff refBookDataDiffDeleted, Structure oldStructure,
                                CompareCriteria criteria, int totalNewCount) {
        if (comparableRows.size() < criteria.getPageSize()) {
            int skipPageCount = criteria.getPageNumber() - totalNewCount / criteria.getPageSize();
            long newDataOnLastPageCount = totalNewCount % criteria.getPageSize();
            long skipDeletedRowsCount = criteria.getPageSize() * skipPageCount - newDataOnLastPageCount;
            long pageSize = skipDeletedRowsCount + criteria.getPageSize();
            if (pageSize <= 0)
                return;
            SearchDataCriteria delSearchDataCriteria = new SearchDataCriteria(0, (int) pageSize, createPrimaryAttributesFilters(refBookDataDiffDeleted, oldStructure));
            Page<RefBookRowValue> delData = versionService.search(criteria.getOldVersionId(), delSearchDataCriteria);
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
                                                        null,
                                                        DiffStatusEnum.DELETED))
                                        .collect(toList())
                        );
                        comparableRows.add(comparableRow);
                    });
        }
    }

    private void validatePrimariesEquality(List<Structure.Attribute> oldPrimaries, List<Structure.Attribute> newPrimaries) {

        if (isEmpty(oldPrimaries))
            throw new UserException(new Message(COMPARE_OLD_PRIMARIES_NOT_FOUND_EXCEPTION_CODE));

        if (isEmpty(newPrimaries))
            throw new UserException(new Message(COMPARE_NEW_PRIMARIES_NOT_FOUND_EXCEPTION_CODE));

        if (oldPrimaries.size() != newPrimaries.size()
                || oldPrimaries.stream().anyMatch(oldPrimary -> newPrimaries.stream().noneMatch(newPrimary -> newPrimary.equals(oldPrimary))))
            throw new UserException(new Message(COMPARE_PRIMARIES_NOT_EQUALS_EXCEPTION_CODE));
    }

    /**
     * Проверка существования пары версий справочника.
     *
     * @param oldVersionId идентификатор старой версии
     * @param newVersionId идентификатор новой версии
     */
    private void validateVersionPairExists(Integer oldVersionId, Integer newVersionId) {

        versionValidation.validateVersionExists(oldVersionId);
        versionValidation.validateVersionExists(newVersionId);
    }
}