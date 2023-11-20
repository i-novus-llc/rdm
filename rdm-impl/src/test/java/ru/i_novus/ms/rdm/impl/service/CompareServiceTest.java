package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableField;
import ru.i_novus.ms.rdm.api.model.compare.ComparableFieldValue;
import ru.i_novus.ms.rdm.api.model.compare.ComparableRow;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.PassportAttributeDiff;
import ru.i_novus.ms.rdm.api.model.diff.PassportDiff;
import ru.i_novus.ms.rdm.api.model.field.CommonField;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.PassportAttribute;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.PassportAttributeRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.diff.CachedDataDiffService;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.CompareDataService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompareServiceTest {

    private static final String PASSPORT_ATTRIBUTE_FULL_NAME = "TEST_fullName";
    private static final String PASSPORT_ATTRIBUTE_SHORT_NAME = "TEST_shortName";
    private static final String PASSPORT_ATTRIBUTE_ANNOTATION = "TEST_annotation";
    private static final String PASSPORT_ATTRIBUTE_GROUP = "TEST_group";
    private static final String PASSPORT_ATTRIBUTE_TYPE = "TEST_type";

    @InjectMocks
    private CompareServiceImpl compareService;

    @Mock
    private CompareDataService compareDataService;

    @Mock
    private CachedDataDiffService cachedDataDiffService;

    @Mock
    private VersionService versionService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private PassportAttributeRepository passportAttributeRepository;

    private static final Integer OLD_ID_P = 1;
    private static final Integer NEW_ID_P = 2;

    private static final Integer OLD_ID = 3;
    private static final Integer NEW_ID = 4;
    private static final Integer OLD_ID_1 = 5;
    private static final Integer NEW_ID_1 = 6;

    private static final Integer DEF_PAGE_SIZE = 4;

    private static RefBookVersionEntity oldVersionP;
    private static RefBookVersionEntity newVersionP;

    private static PassportAttributeEntity passportAttributeFullName;
    private static PassportAttributeEntity passportAttributeShortName;
    private static PassportAttributeEntity passportAttributeAnnotation;
    private static PassportAttributeEntity passportAttributeGroup;
    private static PassportAttributeEntity passportAttributeType;

    private static Structure.Attribute id;
    private static Structure.Attribute code;
    private static Structure.Attribute common;
    private static Structure.Attribute descr;
    private static Structure.Attribute name;
    private static Structure.Attribute upd1;
    private static Structure.Attribute upd2;
    private static Structure.Attribute typeS;
    private static Structure.Attribute typeI;

    private static Field idField;
    private static Field codeField;
    private static Field commonField;

    private static ComparableField idFieldComp;
    private static ComparableField codeFieldComp;
    private static ComparableField commonFieldComp;
    private static ComparableField descrFieldComp;
    private static ComparableField nameFieldComp;
    private static ComparableField upd2FieldComp;
    private static ComparableField typeFieldComp;

    @Before
    public void setUp() {
        initPassportAttributes();
        initAttributes();
        initFields();

        prepareVersions();
        prepareOldVersionData();
        prepareNewVersionData();

        when(passportAttributeRepository.findAllByComparableIsTrueOrderByPositionAsc())
                .thenReturn(asList(passportAttributeFullName, passportAttributeShortName, passportAttributeAnnotation,
                        passportAttributeGroup, passportAttributeType));

        final Structure oldStructure = new Structure(asList(id, code, common, descr, upd1, typeS), emptyList());
        final Structure newStructure = new Structure(asList(id, code, common, name, upd2, typeI), emptyList());
        when(versionService.getStructure(OLD_ID)).thenReturn(oldStructure);
        when(versionService.getStructure(NEW_ID)).thenReturn(newStructure);
        when(versionValidation.equalsPrimaries(eq(oldStructure.getPrimaries()), eq(newStructure.getPrimaries()))).thenReturn(true);

        final Structure oldStructure1 = new Structure(asList(id, common), emptyList());
        final Structure newStructure1 = new Structure(asList(id, common), emptyList());
        when(versionService.getStructure(OLD_ID_1)).thenReturn(oldStructure1);
        when(versionService.getStructure(NEW_ID_1)).thenReturn(newStructure1);
        when(versionValidation.equalsPrimaries(eq(oldStructure1.getPrimaries()), eq(newStructure1.getPrimaries()))).thenReturn(true);
    }

    private void initPassportAttributes() {
        passportAttributeFullName = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_FULL_NAME, "Полное название");
        passportAttributeShortName = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_SHORT_NAME, "Краткое название");
        passportAttributeAnnotation = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_ANNOTATION, "Аннотация");
        passportAttributeGroup = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_GROUP, "Группа");
        passportAttributeType = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_TYPE, "Тип");
    }

    private void initAttributes() {
        id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        code = Structure.Attribute.buildPrimary("CODE", "code", FieldType.STRING, "code");
        common = Structure.Attribute.build("COMMON", "common", FieldType.STRING, "common");
        descr = Structure.Attribute.build("DESCR", "descr", FieldType.STRING, "descr");
        name = Structure.Attribute.build("NAME", "name", FieldType.STRING, "name");
        upd1 = Structure.Attribute.build("UPD", "upd1", FieldType.STRING, "upd");
        upd2 = Structure.Attribute.build("UPD", "upd2", FieldType.STRING, "upd");
        typeS = Structure.Attribute.build("TYPE", "type", FieldType.STRING, "type");
        typeI = Structure.Attribute.build("TYPE", "type", FieldType.INTEGER, "type");
    }

    private void initFields() {
        idField = new CommonField(id.getCode());
        codeField = new CommonField(code.getCode());
        commonField = new CommonField(common.getCode());

        idFieldComp = new ComparableField(id.getCode(), id.getName(), null);
        codeFieldComp = new ComparableField(code.getCode(), code.getName(), null);
        commonFieldComp = new ComparableField(common.getCode(), common.getName(), null);
        descrFieldComp = new ComparableField(descr.getCode(), descr.getName(), DiffStatusEnum.DELETED);
        nameFieldComp = new ComparableField(name.getCode(), name.getName(), DiffStatusEnum.INSERTED);
        upd2FieldComp = new ComparableField(upd2.getCode(), upd2.getName(), DiffStatusEnum.UPDATED);
        typeFieldComp = new ComparableField(typeS.getCode(), typeS.getName(), DiffStatusEnum.UPDATED);
    }

    private void prepareVersions() {
        oldVersionP = new RefBookVersionEntity();
        oldVersionP.setId(OLD_ID_P);
        oldVersionP.setStatus(RefBookVersionStatus.PUBLISHED);

        newVersionP = new RefBookVersionEntity();
        newVersionP.setId(NEW_ID_P);
        newVersionP.setStatus(RefBookVersionStatus.PUBLISHED);

        when(versionRepository.getOne(OLD_ID_P)).thenReturn(oldVersionP);
        when(versionRepository.getOne(NEW_ID_P)).thenReturn(newVersionP);

        RefBookVersionEntity oldVersion = new RefBookVersionEntity();
        oldVersion.setId(OLD_ID);
        oldVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        oldVersion.setStructure(new Structure(asList(id, code, common, descr, upd1, typeS), emptyList()));
        oldVersion.setStorageCode("storage" + OLD_ID);

        RefBookVersionEntity newVersion = new RefBookVersionEntity();
        newVersion.setId(NEW_ID);
        newVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        newVersion.setStructure(new Structure(asList(id, code, common, name, upd2, typeI), emptyList()));
        newVersion.setStorageCode("storage" + NEW_ID);

        when(versionRepository.getOne(OLD_ID)).thenReturn(oldVersion);
        when(versionRepository.getOne(NEW_ID)).thenReturn(newVersion);

        RefBookVersionEntity oldVersion1 = new RefBookVersionEntity();
        oldVersion1.setId(OLD_ID_1);
        oldVersion1.setStatus(RefBookVersionStatus.PUBLISHED);
        oldVersion1.setStructure(new Structure(asList(id, common), emptyList()));
        oldVersion1.setStorageCode("storage" + OLD_ID_1);

        RefBookVersionEntity newVersion1 = new RefBookVersionEntity();
        newVersion1.setId(NEW_ID_1);
        newVersion1.setStatus(RefBookVersionStatus.PUBLISHED);
        newVersion1.setStructure(new Structure(asList(id, common), emptyList()));
        newVersion1.setStorageCode("storage" + NEW_ID_1);

        when(versionRepository.getOne(OLD_ID_1)).thenReturn(oldVersion1);
        when(versionRepository.getOne(NEW_ID_1)).thenReturn(newVersion1);
    }

    private void prepareOldVersionData() {
        PageImpl<RefBookRowValue> oldVersionRows = new PageImpl<>(asList(
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(1)),
                        new StringFieldValue(code.getCode(), "001"),
                        new StringFieldValue(common.getCode(), "c1"),
                        new StringFieldValue(descr.getCode(), "descr1"),
                        new StringFieldValue(upd1.getCode(), "u1"),
                        new StringFieldValue(typeS.getCode(), "1")
                ), OLD_ID),
                new RefBookRowValue(new LongRowValue(new IntegerFieldValue(id.getCode(), BigInteger.valueOf(2)),
                        new StringFieldValue(code.getCode(), "002"),
                        new StringFieldValue(common.getCode(), "c2"),
                        new StringFieldValue(descr.getCode(), "descr2"),
                        new StringFieldValue(upd1.getCode(), "u2"),
                        new StringFieldValue(typeS.getCode(), "2")
                ), OLD_ID),
                new RefBookRowValue(new LongRowValue(new IntegerFieldValue(id.getCode(), BigInteger.valueOf(3)),
                        new StringFieldValue(code.getCode(), "003"),
                        new StringFieldValue(common.getCode(), "c3"),
                        new StringFieldValue(descr.getCode(), "descr3"),
                        new StringFieldValue(upd1.getCode(), "u3"),
                        new StringFieldValue(typeS.getCode(), "3")
                ), OLD_ID)
        ), PageRequest.of(0, 10), 3);

        when(versionService.search(eq(OLD_ID), any(SearchDataCriteria.class))).thenReturn(oldVersionRows);
    }

    private void prepareNewVersionData() {
        PageImpl<RefBookRowValue> newVersionRows = new PageImpl<>(asList(
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(2)),
                        new StringFieldValue(code.getCode(), "002"),
                        new StringFieldValue(common.getCode(), "c2"),
                        new StringFieldValue(name.getCode(), "name2"),
                        new StringFieldValue(upd1.getCode(), "u2"),
                        new IntegerFieldValue(typeS.getCode(), BigInteger.valueOf(2))
                ), NEW_ID),
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(3)),
                        new StringFieldValue(code.getCode(), "003"),
                        new StringFieldValue(common.getCode(), "c3_1"),
                        new StringFieldValue(name.getCode(), "name3"),
                        new StringFieldValue(upd1.getCode(), "u3_1"),
                        new IntegerFieldValue(typeS.getCode(), BigInteger.valueOf(3))
                ), NEW_ID),
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(4)),
                        new StringFieldValue(code.getCode(), "004"),
                        new StringFieldValue(common.getCode(), "c4"),
                        new StringFieldValue(name.getCode(), "name4"),
                        new StringFieldValue(upd1.getCode(), "u4"),
                        new IntegerFieldValue(typeS.getCode(), BigInteger.valueOf(4))
                ), NEW_ID)
        ), PageRequest.of(0, 10), 3);

        when(versionService.search(eq(NEW_ID), any(SearchDataCriteria.class))).thenReturn(newVersionRows);
    }

    @Test
    public void testComparePassports() {
        List<PassportValueEntity> oldPassportValues = new ArrayList<>();
        oldPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", oldVersionP));
        oldPassportValues.add(new PassportValueEntity(passportAttributeShortName, "short_name", oldVersionP));
        oldPassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", oldVersionP));
        oldPassportValues.add(new PassportValueEntity(passportAttributeType, null, oldVersionP));
        oldVersionP.setPassportValues(oldPassportValues);

        List<PassportValueEntity> newPassportValues = new ArrayList<>();
        newPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name_upd", newVersionP));
        newPassportValues.add(new PassportValueEntity(passportAttributeAnnotation, "annotation", newVersionP));
        newPassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", newVersionP));
        newPassportValues.add(new PassportValueEntity(passportAttributeType, null, newVersionP));
        newVersionP.setPassportValues(newPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(oldVersionP.getId(), newVersionP.getId());

        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                "full_name",
                "full_name_upd"
        ));
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeShortName.getCode(), passportAttributeShortName.getName()),
                "short_name",
                null
        ));
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeAnnotation.getCode(), passportAttributeAnnotation.getName()),
                null,
                "annotation"
        ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenUpdateAttributeValue() {
        List<PassportValueEntity> oldPassportValues = new ArrayList<>();
        oldPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", oldVersionP));
        oldVersionP.setPassportValues(oldPassportValues);

        List<PassportValueEntity> newPassportValues = new ArrayList<>();
        newPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name_upd", newVersionP));
        newVersionP.setPassportValues(newPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(oldVersionP.getId(), newVersionP.getId());

        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                "full_name",
                "full_name_upd"
        ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenAddAttributeValue() {
        List<PassportValueEntity> oldPassportValues = new ArrayList<>();
        oldVersionP.setPassportValues(oldPassportValues);

        List<PassportValueEntity> newPassportValues = new ArrayList<>();
        newPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", newVersionP));
        newVersionP.setPassportValues(newPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(oldVersionP.getId(), newVersionP.getId());
        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                null,
                "full_name"
        ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenDeleteAttributeValue() {
        List<PassportValueEntity> oldPassportValues = new ArrayList<>();
        oldPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", oldVersionP));
        oldVersionP.setPassportValues(oldPassportValues);

        List<PassportValueEntity> newPassportValues = new ArrayList<>();
        newVersionP.setPassportValues(newPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(oldVersionP.getId(), newVersionP.getId());
        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                "full_name",
                null
        ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenNoDiff() {
        List<PassportValueEntity> oldPassportValues = new ArrayList<>();
        oldPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", oldVersionP));
        oldVersionP.setPassportValues(oldPassportValues);

        List<PassportValueEntity> newPassportValues = new ArrayList<>();
        newPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", newVersionP));
        newVersionP.setPassportValues(newPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(oldVersionP.getId(), newVersionP.getId());
        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    /*
     * test getting rows for both versions with applied diff in one structure for merged display
     * deleted rows must be moved to the end of the list
     * deleted fields must be moved to the end of the fields list
     * displayed structure's fields are sorted be new (right) version
     * displayed rows are sorted by new (right) version
     * in this test: total rows count (new + deleted) is less than page size
     */
    @Test
    public void testCommonDataDiffOnePageWithNewAndDeletedRows() {
        CompareDataCriteria compareDataCriteria = createRdmDefaultCompareDataCriteria(OLD_ID, NEW_ID);
        Page<ComparableRow> expectedCommonComparableRows = new RestPage<>(asList(
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(2), BigInteger.valueOf(2), null),
                        new ComparableFieldValue(codeFieldComp, "002", "002", null),
                        new ComparableFieldValue(commonFieldComp, "c2", "c2", null),
                        new ComparableFieldValue(nameFieldComp, null, "name2", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(upd2FieldComp, "u2", "u2", null),
                        new ComparableFieldValue(typeFieldComp, "2", BigInteger.valueOf(2), null),
                        new ComparableFieldValue(descrFieldComp, "descr2", null, DiffStatusEnum.DELETED)
                ),
                        null),
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(3), BigInteger.valueOf(3), null),
                        new ComparableFieldValue(codeFieldComp, "003", "003", null),
                        new ComparableFieldValue(commonFieldComp, "c3", "c3_1", DiffStatusEnum.UPDATED),
                        new ComparableFieldValue(nameFieldComp, null, "name3", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(upd2FieldComp, "u3", "u3_1", DiffStatusEnum.UPDATED),
                        new ComparableFieldValue(typeFieldComp, "3", BigInteger.valueOf(3), null),
                        new ComparableFieldValue(descrFieldComp, "descr3", null, DiffStatusEnum.DELETED)
                ),
                        DiffStatusEnum.UPDATED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, null, BigInteger.valueOf(4), DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(codeFieldComp, null, "004", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(commonFieldComp, null, "c4", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(nameFieldComp, null, "name4", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(upd2FieldComp, null, "u4", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(typeFieldComp, null, BigInteger.valueOf(4), DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(descrFieldComp, null, null, null, DiffStatusEnum.INSERTED)
                ),
                        DiffStatusEnum.INSERTED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(1), null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(codeFieldComp, "001", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(commonFieldComp, "c1", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(upd2FieldComp, "u1", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(typeFieldComp, "1", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(descrFieldComp, "descr1", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(descrFieldComp, "descr1", null, DiffStatusEnum.DELETED)
                ),
                        DiffStatusEnum.DELETED)
        ), compareDataCriteria, 4);

        PageImpl<RefBookRowValue> deletedRows = new PageImpl<>(singletonList(
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(1)),
                        new StringFieldValue(code.getCode(), "001"),
                        new StringFieldValue(common.getCode(), "c1"),
                        new StringFieldValue(descr.getCode(), "descr1"),
                        new StringFieldValue(upd1.getCode(), "u1"),
                        new StringFieldValue(typeS.getCode(), "1")
                ), NEW_ID)
        ), PageRequest.of(0, 10), 1);
        when(versionService.search(eq(OLD_ID), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(0, 1)))))
                .thenReturn(deletedRows);

        List<DiffRowValue> diffRowValuesList = new ArrayList<>();
        diffRowValuesList.add(new DiffRowValue(asList(
                new DiffFieldValue<>(idField, null, BigInteger.valueOf(3), null),
                new DiffFieldValue<>(codeField, null, "003", null),
                new DiffFieldValue<>(commonField, "c3", "c3_1", DiffStatusEnum.UPDATED)
        ), DiffStatusEnum.UPDATED));
        diffRowValuesList.add(new DiffRowValue(asList(
                new DiffFieldValue<>(idField, null, BigInteger.valueOf(4), null),
                new DiffFieldValue<>(codeField, null, "004", null),
                new DiffFieldValue<>(commonField, null, "c4", DiffStatusEnum.INSERTED)
        ), DiffStatusEnum.INSERTED));

        when(compareDataService.getDataDifference(any(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria.class)))
                .thenReturn(new DataDifference(
                        new DataPage<>(diffRowValuesList.size(), diffRowValuesList, createCriteria(0, DEF_PAGE_SIZE, 3))
                ));

        List<DiffRowValue> deletedDiffRowValuesList = new ArrayList<>();
        deletedDiffRowValuesList.add(new DiffRowValue(asList(
                new DiffFieldValue<>(idField, BigInteger.valueOf(1), null, null),
                new DiffFieldValue<>(codeField, "001", null, null),
                new DiffFieldValue<>(commonField, "c1", null, null)
        ), DiffStatusEnum.DELETED));


        when(compareDataService.getDataDifference(argThat(new CompareDataCriteriaMatcher(createVdsDeletedCompareDataCriteria(OLD_ID, NEW_ID)))))
                .thenReturn(new DataDifference(
                        new DataPage<>(deletedDiffRowValuesList.size(), deletedDiffRowValuesList, createCriteria(0, DEF_PAGE_SIZE, 1))
                ));

        Page<ComparableRow> actualCommonComparableRows = compareService.getCommonComparableRows(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonComparableRows, actualCommonComparableRows);
    }

    /*
     * test getting rows for both versions with applied diff in one structure for merged display
     * two fully filled pages: first page with new (inserted) rows, second - with old (deleted) rows
     * count of new and old data is equal to page size
     */
    @Test
    public void testCommonDataDiffExactDivisionOnPagesWithNewAndDeletedRows() {

        List<RefBookRowValue> oldVersionRows = Stream.of(5, 6, 7, 8)
                .map(index -> new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "old" + index)
                ), OLD_ID_1))
                .collect(Collectors.toList());

        List<RefBookRowValue> newVersionRows = Stream.of(1, 2, 3, 4)
                .map(index -> new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "new" + index)
                ), NEW_ID_1))
                .collect(Collectors.toList());

        when(versionService
                .search(eq(NEW_ID_1), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(emptyList(), PageRequest.of(0, DEF_PAGE_SIZE), 4));
        when(versionService
                .search(eq(NEW_ID_1), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(0, DEF_PAGE_SIZE)))))
                .thenReturn(new PageImpl<>(newVersionRows.subList(0, 4), PageRequest.of(0, DEF_PAGE_SIZE), 4));

        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(0, DEF_PAGE_SIZE)))))
                .thenReturn(new PageImpl<>(oldVersionRows, PageRequest.of(0, DEF_PAGE_SIZE), 4));

//        test first page
        CompareDataCriteria compareDataCriteria = createRdmDefaultCompareDataCriteria(OLD_ID_1, NEW_ID_1);
        ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria compareDataCriteriaDeletedVds = createVdsDeletedCompareDataCriteria(OLD_ID_1, NEW_ID_1);
        Page<ComparableRow> expectedCommonComparableRowsPage1 = new RestPage<>(createListOfNewComparableRows(1, 2, 3, 4), compareDataCriteria, 8);

        List<DiffRowValue> diffRowValuesListPage1 = createListOfNewDiffRowValues(1, 2, 3, 4);

        when(compareDataService.getDataDifference(any(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria.class)))
                .thenReturn(new DataDifference(
                        new DataPage<>(diffRowValuesListPage1.size(), diffRowValuesListPage1, createCriteria(0, DEF_PAGE_SIZE, 8))
                ));

        Page<ComparableRow> actualCommonComparableRowsPage1 = compareService.getCommonComparableRows(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonComparableRowsPage1, actualCommonComparableRowsPage1);

//        test second page
        compareDataCriteria.setPageNumber(1);
        Page<ComparableRow> expectedCommonComparableRowsPage2 = new RestPage<>(createListOfOldComparableRows(5, 6, 7, 8), compareDataCriteria, 8);

        List<DiffRowValue> diffRowValuesListPage2 = createListOfOldDiffRowValues(5, 6, 7, 8);

        when(compareDataService.getDataDifference(any(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria.class)))
                .thenReturn(new DataDifference(
                        new DataPage<>(diffRowValuesListPage2.size(), diffRowValuesListPage2, createCriteria(1, DEF_PAGE_SIZE, 8))
                ));
        when(compareDataService.getDataDifference(argThat(new CompareDataCriteriaMatcher(compareDataCriteriaDeletedVds))))
                .thenReturn(new DataDifference(
                        new DataPage<>(4, emptyList(), createCriteria(0, 10, 4))
                ));
        Page<ComparableRow> actualCommonComparableRowsPage2 = compareService.getCommonComparableRows(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonComparableRowsPage2, actualCommonComparableRowsPage2);
    }

    /*
     * test getting rows for both versions with applied diff in one structure for merged display
     * there are several pages with new (inserted) and old (deleted) rows that are mixed on second page
     */
    @Test
    public void testCommonDataDiffSeveralPagesWithNewAndDeletedRows() {

        List<RefBookRowValue> newVersionRows = Stream.of(1, 2, 3, 4, 5)
                .map(index -> new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "new" + index)
                ), NEW_ID_1))
                .collect(Collectors.toList());

        List<RefBookRowValue> oldVersionRows = Stream.of(6, 7, 8, 9, 10, 11, 12, 13)
                .map(index -> new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "old" + index)
                ), OLD_ID_1))
                .collect(Collectors.toList());

        when(versionService
                .search(eq(NEW_ID_1), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(emptyList(), PageRequest.of(0, DEF_PAGE_SIZE), 5));
        when(versionService
                .search(eq(NEW_ID_1), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(0, DEF_PAGE_SIZE)))))
                .thenReturn(new PageImpl<>(newVersionRows.subList(0, 4), PageRequest.of(0, DEF_PAGE_SIZE), 5));
        when(versionService
                .search(eq(NEW_ID_1), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(1, DEF_PAGE_SIZE)))))
                .thenReturn(new PageImpl<>(newVersionRows.subList(4, 5), PageRequest.of(1, DEF_PAGE_SIZE), 5));

        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(0, 3)))))
                .thenReturn(new PageImpl<>(oldVersionRows.subList(0, 3), PageRequest.of(0, 3), 8));
        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(0, 7)))))
                .thenReturn(new PageImpl<>(oldVersionRows.subList(0, 7), PageRequest.of(0, 7), 8));
        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(new SearchDataCriteria(0, 11)))))
                .thenReturn(new PageImpl<>(oldVersionRows, PageRequest.of(0, 11), 8));

        ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria compareDataCriteriaDeletedVds = createVdsDeletedCompareDataCriteria(OLD_ID_1, NEW_ID_1);
//        test first page
        CompareDataCriteria compareDataCriteria = createRdmDefaultCompareDataCriteria(OLD_ID_1, NEW_ID_1);
        Page<ComparableRow> expectedCommonComparableRowsPage1 = new RestPage<>(createListOfNewComparableRows(1, 2, 3, 4), compareDataCriteria, 13);

        List<DiffRowValue> diffRowValuesListPage1 = createListOfNewDiffRowValues(1, 2, 3, 4);

        when(compareDataService.getDataDifference(any(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria.class)))
                .thenReturn(new DataDifference(
                        new DataPage<>(diffRowValuesListPage1.size(), diffRowValuesListPage1, createCriteria(0, DEF_PAGE_SIZE, 13))
                ));
        when(compareDataService.getDataDifference(argThat(new CompareDataCriteriaMatcher(compareDataCriteriaDeletedVds))))
                .thenReturn(new DataDifference(new DataPage<>(8, emptyList(), createCriteria(0, 10, 8))));

        Page<ComparableRow> actualCommonComparableRowsPage1 = compareService.getCommonComparableRows(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonComparableRowsPage1, actualCommonComparableRowsPage1);

//        test second page
        compareDataCriteria.setPageNumber(1);
        List<ComparableRow> expectedComparableRowsListPage2 = new ArrayList<>();
        expectedComparableRowsListPage2.addAll(createListOfNewComparableRows(5));
        expectedComparableRowsListPage2.addAll(createListOfOldComparableRows(6, 7, 8));
        Page<ComparableRow> expectedCommonComparableRowsPage2 = new RestPage<>(expectedComparableRowsListPage2, compareDataCriteria, 13);

        List<DiffRowValue> diffRowValuesListPage2 = new ArrayList<>();
        diffRowValuesListPage2.addAll(createListOfNewDiffRowValues(5));
        diffRowValuesListPage2.addAll(createListOfOldDiffRowValues(6, 7, 8));

        when(compareDataService.getDataDifference(any(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria.class)))
                .thenReturn(new DataDifference(
                        new DataPage<>(diffRowValuesListPage2.size(), diffRowValuesListPage2, createCriteria(1, DEF_PAGE_SIZE, 13))
                ));
        when(compareDataService.getDataDifference(argThat(new CompareDataCriteriaMatcher(compareDataCriteriaDeletedVds))))
                .thenReturn(new DataDifference(new DataPage<>(8, emptyList(), createCriteria(0, 10, 8))));

        Page<ComparableRow> actualCommonComparableRowsPage2 = compareService.getCommonComparableRows(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonComparableRowsPage2, actualCommonComparableRowsPage2);

//        test third page
        compareDataCriteria.setPageNumber(2);
        Page<ComparableRow> expectedCommonComparableRowsPage3 = new RestPage<>(createListOfOldComparableRows(9, 10, 11, 12), compareDataCriteria, 13);

        List<DiffRowValue> diffRowValuesListPage3 = createListOfOldDiffRowValues(9, 10, 11, 12);

        when(compareDataService.getDataDifference(any(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria.class)))
                .thenReturn(new DataDifference(
                        new DataPage<>(diffRowValuesListPage3.size(), diffRowValuesListPage3, createCriteria(2, DEF_PAGE_SIZE, 13))
                ));
        when(compareDataService.getDataDifference(argThat(new CompareDataCriteriaMatcher(compareDataCriteriaDeletedVds))))
                .thenReturn(new DataDifference(new DataPage<>(8, emptyList(), createCriteria(0, 10, 8))));

        Page<ComparableRow> actualCommonDataDiffPage3 = compareService.getCommonComparableRows(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonComparableRowsPage3, actualCommonDataDiffPage3);

//        test forth page
        compareDataCriteria.setPageNumber(3);
        Page<ComparableRow> expectedCommonComparableRowsPage4 = new RestPage<>(createListOfOldComparableRows(13), compareDataCriteria, 13);

        List<DiffRowValue> diffRowValuesListPage4 = createListOfOldDiffRowValues(13);

        when(compareDataService.getDataDifference(any(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria.class)))
                .thenReturn(new DataDifference(
                        new DataPage<>(diffRowValuesListPage4.size(), diffRowValuesListPage4, createCriteria(3, DEF_PAGE_SIZE, 13))
                ));
        when(compareDataService.getDataDifference(argThat(new CompareDataCriteriaMatcher(compareDataCriteriaDeletedVds))))
                .thenReturn(new DataDifference(new DataPage<>(8, emptyList(), createCriteria(0, 10, 8))));

        Page<ComparableRow> actualCommonDataDiffPage4 = compareService.getCommonComparableRows(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonComparableRowsPage4, actualCommonDataDiffPage4);
    }

    private List<ComparableRow> createListOfNewComparableRows(Integer... indexes) {
        return Stream.of(indexes)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, null, BigInteger.valueOf(index), DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(commonFieldComp, null, "new" + index, DiffStatusEnum.INSERTED)
                        ), DiffStatusEnum.INSERTED)
                )
                .collect(Collectors.toList());
    }

    private List<ComparableRow> createListOfOldComparableRows(Integer... indexes) {
        return Stream.of(indexes)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, BigInteger.valueOf(index), null, DiffStatusEnum.DELETED),
                                new ComparableFieldValue(commonFieldComp, "old" + index, null, DiffStatusEnum.DELETED)
                        ), DiffStatusEnum.DELETED)
                )
                .collect(Collectors.toList());
    }

    private List<DiffRowValue> createListOfNewDiffRowValues(Integer... indexes) {
        return Stream.of(indexes)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, null, BigInteger.valueOf(index), DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(commonField, null, "new" + index, DiffStatusEnum.INSERTED)
                ), DiffStatusEnum.INSERTED))
                .collect(Collectors.toList());
    }

    private List<DiffRowValue> createListOfOldDiffRowValues(Integer... indexes) {
        return Stream.of(indexes)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, BigInteger.valueOf(index), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(commonField, "old" + index, null, DiffStatusEnum.DELETED)
                ), DiffStatusEnum.DELETED))
                .collect(Collectors.toList());
    }

    private void assertPassportDiffs(PassportDiff expectedPassportDiff, PassportDiff actualPassportDiff) {
        assertEquals(expectedPassportDiff.getPassportAttributeDiffs().size(), actualPassportDiff.getPassportAttributeDiffs().size());
        expectedPassportDiff.getPassportAttributeDiffs().forEach(expectedPassportAttributeDiff -> {
            PassportAttributeDiff actualPassportAttributeDiff = actualPassportDiff.getPassportAttributeDiffs().stream().filter(passportAttributeDiff ->
                    expectedPassportAttributeDiff.getPassportAttribute().getCode().equals(passportAttributeDiff.getPassportAttribute().getCode())).findFirst().orElse(null);
            if (actualPassportAttributeDiff == null)
                Assert.fail("Attribute \"" + expectedPassportAttributeDiff.getPassportAttribute().getName() + "\" must be in diff");
            assertEquals(expectedPassportAttributeDiff.getOldValue(), actualPassportAttributeDiff.getOldValue());
            assertEquals(expectedPassportAttributeDiff.getNewValue(), actualPassportAttributeDiff.getNewValue());
        });
    }

    /*
     * assert that right rows are in the right order
     */
    private void assertComparableRowsEquals(Page<ComparableRow> expectedRowsWithDiff, Page<ComparableRow> actualRowsWithDiff) {
        assertEquals(expectedRowsWithDiff.getContent().size(), actualRowsWithDiff.getContent().size());
        assertEquals(expectedRowsWithDiff.getTotalElements(), actualRowsWithDiff.getTotalElements());

        for (int i = 0; i < expectedRowsWithDiff.getContent().size(); i++) {
            ComparableRow expectedRow = expectedRowsWithDiff.getContent().get(i);
            ComparableRow actualRow = actualRowsWithDiff.getContent().get(i);
            assertEquals(expectedRow.getStatus(), actualRow.getStatus());
            assertEquals(expectedRow.getFieldValues().size(), actualRow.getFieldValues().size());
            assertTrue(actualRow.getFieldValues().containsAll(expectedRow.getFieldValues()));
        }
    }

    private DataCriteria createCriteria(int pageNumber, int pageSize, Integer count) {

        final DataCriteria criteria = new DataCriteria();
        criteria.setPage(pageNumber + 1);
        criteria.setSize(pageSize);
        criteria.setCount(count);
        return criteria;
    }

    private CompareDataCriteria createRdmDefaultCompareDataCriteria(Integer oldId, Integer newId) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(oldId, newId);
        compareDataCriteria.setPageSize(DEF_PAGE_SIZE);
        return compareDataCriteria;
    }

    private ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria createVdsDeletedCompareDataCriteria(Integer oldId, Integer newId) {

        ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria compareDataCriteria = new ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria("storage" + oldId, "storage" + newId);

        compareDataCriteria.setCountOnly(false);
        compareDataCriteria.setStatus(DiffStatusEnum.DELETED);
        compareDataCriteria.setPage(1);
        compareDataCriteria.setSize(10);

        return compareDataCriteria;
    }

    /*
     * suppose that two vds CompareDataCriteria values are equal for mocking if equal version ids, countOnly flag and diffStatus
     * ignore page size and page number (from Criteria)
     */
    private static class CompareDataCriteriaMatcher implements ArgumentMatcher<ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria> {

        private ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria expected;

        CompareDataCriteriaMatcher(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria criteria) {
            this.expected = criteria;
        }

        @Override
        public boolean matches(ru.i_novus.platform.datastorage.temporal.model.criteria.CompareDataCriteria actual) {
            if (actual == null)
                return false;

            return expected.getStorageCode().equals(actual.getStorageCode()) &&
                    expected.getNewStorageCode().equals(actual.getNewStorageCode()) &&
                    expected.getCountOnly() == actual.getCountOnly() &&
                    expected.getStatus() == actual.getStatus();
        }
    }

    /*
     * suppose that two SearchDataCriteria values are equal for mocking if equal page size and page number
     * ignore attribute filters, common filter and fields filter
     */
    private static class SearchDataCriteriaMatcher implements ArgumentMatcher<SearchDataCriteria> {

        private SearchDataCriteria expected;

        SearchDataCriteriaMatcher(SearchDataCriteria criteria) {
            this.expected = criteria;
        }

        @Override
        public boolean matches(SearchDataCriteria actual) {
            if (actual == null)
                return false;

            return expected.getPageSize() == actual.getPageSize() &&
                    expected.getPageNumber() == actual.getPageNumber();
        }
    }
}