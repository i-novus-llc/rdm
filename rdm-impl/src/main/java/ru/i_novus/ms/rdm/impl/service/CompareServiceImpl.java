package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.*;
import ru.i_novus.ms.rdm.api.model.diff.*;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.PassportAttribute;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.PassportAttributeRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.diff.CachedDataDiffService;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.BaseDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.CompareDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.ComparableUtils.*;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;

@Service
@Primary
@SuppressWarnings({"rawtypes", "java:S3740"})
public class CompareServiceImpl implements CompareService {

    private static final String COMPARE_OLD_VERSION_PRIMARIES_NOT_FOUND_EXCEPTION_CODE = "compare.old.version.primaries.not.found";
    private static final String COMPARE_NEW_VERSION_PRIMARIES_NOT_FOUND_EXCEPTION_CODE = "compare.new.version.primaries.not.found";
    private static final String COMPARE_VERSIONS_PRIMARIES_NOT_MATCH_EXCEPTION_CODE = "compare.versions.primaries.not.match";
    private static final String COMPARE_REFBOOKS_PRIMARIES_NOT_MATCH_EXCEPTION_CODE = "compare.refbooks.primaries.not.match";

    private final CompareDataService compareDataService;
    private final VersionService versionService;
    private final CachedDataDiffService cachedDataDiffService;

    private final RefBookVersionRepository versionRepository;
    private final PassportAttributeRepository passportAttributeRepository;

    private final VersionValidation versionValidation;

    @Autowired
    public CompareServiceImpl(CompareDataService compareDataService,
                              VersionService versionService,
                              CachedDataDiffService cachedDataDiffService,
                              RefBookVersionRepository versionRepository,
                              PassportAttributeRepository passportAttributeRepository,
                              VersionValidation versionValidation) {
        this.compareDataService = compareDataService;
        this.versionService = versionService;
        this.cachedDataDiffService = cachedDataDiffService;

        this.versionRepository = versionRepository;
        this.passportAttributeRepository = passportAttributeRepository;

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
            PassportValueEntity oldPassportValue = oldVersion.getPassportValue(passportAttribute);
            PassportValueEntity newPassportValue = newVersion.getPassportValue(passportAttribute);

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
    public RefBookDataDiff compareData(ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria criteria) {

        validateVersionPairExists(criteria.getOldVersionId(), criteria.getNewVersionId());

        RefBookVersionEntity oldVersion = versionRepository.getOne(criteria.getOldVersionId());
        RefBookVersionEntity newVersion = versionRepository.getOne(criteria.getNewVersionId());
        validatePrimariesEquality(oldVersion, newVersion);

        RefBookAttributeDiff attributeDiff = compareAttributes(oldVersion.getStructure(), newVersion.getStructure());

        DataDifference dataDifference = null;
        if (Boolean.TRUE.equals(criteria.getUseCached())) {
            dataDifference = cachedDataDiffService.getCachedDataDifference(criteria, attributeDiff);
        }
        if (dataDifference == null) {
            CompareDataCriteria compareDataCriteria = createVdsCompareDataCriteria(oldVersion, newVersion, criteria);
            dataDifference = compareDataService.getDataDifference(compareDataCriteria);
        }
        return new RefBookDataDiff(new DiffRowValuePage(dataDifference.getRows()), attributeDiff);
    }

    private RefBookAttributeDiff compareAttributes(Structure oldStructure, Structure newStructure) {

        List<String> newAttributes = new ArrayList<>();
        List<String> oldAttributes = new ArrayList<>();
        List<String> updatedAttributes = new ArrayList<>();

        newStructure.getAttributes().forEach(newAttribute -> {
            Structure.Attribute oldAttribute = oldStructure.getAttribute(newAttribute.getCode());
            if (oldAttribute == null)
                newAttributes.add(newAttribute.getCode());
            else if (!attributeEquals(oldAttribute, newAttribute))
                updatedAttributes.add(newAttribute.getCode());
        });

        oldStructure.getAttributes().forEach(oldAttribute -> {
            Structure.Attribute newAttribute = newStructure.getAttribute(oldAttribute.getCode());
            if (newAttribute == null)
                oldAttributes.add(oldAttribute.getCode());
        });

        return new RefBookAttributeDiff(oldAttributes, newAttributes, updatedAttributes);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComparableRow> getCommonComparableRows(ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria criteria) {

        validateVersionPairExists(criteria.getNewVersionId(), criteria.getOldVersionId());

        Structure newStructure = versionService.getStructure(criteria.getNewVersionId());
        Structure oldStructure = versionService.getStructure(criteria.getOldVersionId());

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize());
        searchDataCriteria.setAttributeFilters(criteria.getPrimaryAttributesFilters());
        Page<RefBookRowValue> newData = versionService.search(criteria.getNewVersionId(), searchDataCriteria);

        RefBookDataDiff dataDiff = compareData(createRdmCompareDataCriteria(criteria, newData, newStructure));
        RefBookDataDiff deletedDiff = compareData(createRdmDeletedDataCriteria(criteria));

        List<ComparableField> comparableFields = createCommonComparableFields(dataDiff.getAttributeDiff(), newStructure, oldStructure);
        List<ComparableRow> comparableRows = new ArrayList<>();

        addNewVersionRows(comparableRows, comparableFields, newData, dataDiff, newStructure, criteria);
        addDeletedRows(comparableRows, comparableFields, deletedDiff, oldStructure, criteria, (int) newData.getTotalElements());

        return new RestPage<>(comparableRows, criteria, newData.getTotalElements() + deletedDiff.getRows().getTotalElements());
    }

    private List<Field> getCommonFields(Structure oldStructure, Structure newStructure) {

        return newStructure.getAttributes().stream()
                .filter(newAttribute -> {
                    Structure.Attribute oldAttribute = oldStructure.getAttribute(newAttribute.getCode());
                    return attributeEquals(oldAttribute, newAttribute);
                })
                .map(attribute -> ConverterUtil.field(attribute.getCode(), attribute.getType()))
                .collect(toList());
    }

    /** Сравнение атрибутов только по полям, связанным с изменением атрибута. */
    private boolean attributeEquals(Structure.Attribute oldAttribute, Structure.Attribute newAttribute) {
        return oldAttribute != null
                && oldAttribute.storageEquals(newAttribute)
                && Objects.equals(oldAttribute.getName(), newAttribute.getName());
    }

    private boolean equalValues(PassportValueEntity oldPassportValue, PassportValueEntity newPassportValue) {
        if (oldPassportValue != null && oldPassportValue.getValue() != null)
            return (newPassportValue != null && oldPassportValue.getValue().equals(newPassportValue.getValue()));
        else
            return (newPassportValue == null || newPassportValue.getValue() == null);
    }

    private CompareDataCriteria createVdsCompareDataCriteria(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion,
                                                             ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria rdmCriteria) {

        CompareDataCriteria vdsCriteria = new CompareDataCriteria(oldVersion.getStorageCode(), newVersion.getStorageCode());

        vdsCriteria.setFields(getCommonFields(oldVersion.getStructure(), newVersion.getStructure()));
        vdsCriteria.setPrimaryFields(Structure.getAttributeCodes(newVersion.getStructure().getPrimaries()).collect(toList()));
        vdsCriteria.setPrimaryFieldsFilters(toFieldSearchCriterias(rdmCriteria.getPrimaryAttributesFilters()));

        vdsCriteria.setOldPublishDate(oldVersion.getFromDate());
        vdsCriteria.setOldCloseDate(oldVersion.getToDate());
        vdsCriteria.setNewPublishDate(newVersion.getFromDate());
        vdsCriteria.setNewCloseDate(newVersion.getToDate());

        vdsCriteria.setCountOnly(rdmCriteria.getCountOnly() != null && rdmCriteria.getCountOnly());
        vdsCriteria.setStatus(rdmCriteria.getDiffStatus());
        vdsCriteria.setPage(rdmCriteria.getPageNumber() + BaseDataCriteria.PAGE_SHIFT);
        vdsCriteria.setSize(rdmCriteria.getPageSize());

        return vdsCriteria;
    }

    private ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria createRdmCompareDataCriteria(CompareCriteria criteria, Page<? extends RowValue> data, Structure structure) {

        ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria rdmCriteria = new ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria(criteria);
        rdmCriteria.setPrimaryAttributesFilters(createPrimaryAttributesFilters(data, structure));
        rdmCriteria.setUseCached(Boolean.FALSE); // Кеш отключён, т.к. в нём пока нет фильтрации по DiffStatus.

        return rdmCriteria;
    }

    private ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria createRdmDeletedDataCriteria(CompareCriteria criteria) {

        ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria rdmCriteria = new ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria(criteria);
        rdmCriteria.setPrimaryAttributesFilters(emptySet());
        rdmCriteria.setDiffStatus(DiffStatusEnum.DELETED);
        rdmCriteria.setCountOnly(false);
        rdmCriteria.setUseCached(Boolean.FALSE); // Кеш отключён, т.к. в нём пока нет фильтрации по DiffStatus.

        return rdmCriteria;
    }

    private void addNewVersionRows(List<ComparableRow> comparableRows, List<ComparableField> comparableFields,
                                   Page<? extends RowValue> newData, RefBookDataDiff refBookDataDiff,
                                   Structure newStructure, CompareCriteria criteria) {
        if (isEmpty(newData.getContent()))
            return;

        RefBookAttributeDiff attributeDiff = refBookDataDiff.getAttributeDiff();

        boolean hasUpdOrDelAttr = !isEmpty(attributeDiff.getUpdatedAttributes()) || !isEmpty(attributeDiff.getOldAttributes());

        Page<RefBookRowValue> oldData = null;
        if (hasUpdOrDelAttr) {
            SearchDataCriteria oldSearchDataCriteria = new SearchDataCriteria();
            oldSearchDataCriteria.setPageSize(criteria.getPageSize());
            oldSearchDataCriteria.setAttributeFilters(createPrimaryAttributesFilters(newData, newStructure));

            oldData = versionService.search(criteria.getOldVersionId(), oldSearchDataCriteria);
        }

        final List<RefBookRowValue> oldContent = oldData != null ? oldData.getContent() : null;
        newData.getContent()
                .forEach(newRowValue -> {
                    ComparableRow comparableRow = new ComparableRow();
                    DiffRowValue diffRowValue = findDiffRowValue(newStructure.getPrimaries(), newRowValue,
                            refBookDataDiff.getRows().getContent());
                    RowValue oldRowValue = oldContent != null
                            ? findRowValue(newStructure.getPrimaries(), newRowValue, oldContent)
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
                                RefBookDataDiff deletedDiff, Structure oldStructure,
                                CompareCriteria criteria, int totalNewCount) {
        if (comparableRows.size() < criteria.getPageSize()) {

            int skipPageCount = criteria.getPageNumber() - totalNewCount / criteria.getPageSize();
            long newDataOnLastPageCount = totalNewCount % criteria.getPageSize();
            long skipDeletedRowsCount = (long) criteria.getPageSize() * skipPageCount - newDataOnLastPageCount;

            long pageSize = skipDeletedRowsCount + criteria.getPageSize();
            if (pageSize <= 0)
                return;

            SearchDataCriteria delSearchDataCriteria = new SearchDataCriteria();
            delSearchDataCriteria.setPageSize((int) pageSize);
            delSearchDataCriteria.setAttributeFilters(createPrimaryAttributesFilters(deletedDiff, oldStructure));

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

    /** Проверка первичных ключей версий на совпадение. */
    private void validatePrimariesEquality(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion) {

        List<Structure.Attribute> oldPrimaries = oldVersion.getStructure().getPrimaries();
        if (isEmpty(oldPrimaries))
            throw new UserException(new Message(COMPARE_OLD_VERSION_PRIMARIES_NOT_FOUND_EXCEPTION_CODE, oldVersion.getRefBook().getCode(), oldVersion.getVersion()));

        List<Structure.Attribute> newPrimaries = newVersion.getStructure().getPrimaries();
        if (isEmpty(newPrimaries))
            throw new UserException(new Message(COMPARE_NEW_VERSION_PRIMARIES_NOT_FOUND_EXCEPTION_CODE, newVersion.getRefBook().getCode(), newVersion.getVersion()));

        if (!versionValidation.equalsPrimaries(oldPrimaries, newPrimaries)) {
            if (newVersion.getRefBook().getCode().equals(oldVersion.getRefBook().getCode())) {
                throw new UserException(new Message(COMPARE_VERSIONS_PRIMARIES_NOT_MATCH_EXCEPTION_CODE,
                        oldVersion.getRefBook().getCode(), oldVersion.getVersionNumber(), newVersion.getVersionNumber()));
            } else {
                throw new UserException(new Message(COMPARE_REFBOOKS_PRIMARIES_NOT_MATCH_EXCEPTION_CODE,
                        oldVersion.getRefBook().getCode(), oldVersion.getVersionNumber(),
                        newVersion.getRefBook().getCode(), newVersion.getVersionNumber()));
            }
        }
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