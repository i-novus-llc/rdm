package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.platform.jaxrs.RestPage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.*;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.VersionService;

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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompareDataControllerTest {

    @InjectMocks
    private CompareDataController compareDataController;

    @Mock
    private VersionService versionService;
    @Mock
    private CompareService compareService;

    private static final Integer OLD_ID = 1;
    private static final Integer NEW_ID = 2;
    private static final Integer OLD_ID_1 = 3;
    private static final Integer NEW_ID_1 = 4;

    private static Criteria criteria;
    private static CompareDataCriteria compareDataCriteria;
    private static CompareDataCriteria deletedCompareDataCriteria;
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

    @Before
    public void init() {
        initCriterias();
        initAttributes();
        initFields();
        prepareRefBookDataDiff();
        prepareRefBookDeletedDataDiff();
        prepareOldVersionData();
        prepareNewVersionData();

        when(versionService.getStructure(OLD_ID)).thenReturn(new Structure(asList(id, code, common, descr, upd1, typeS), emptyList()));
        when(versionService.getStructure(NEW_ID)).thenReturn(new Structure(asList(id, code, common, name, upd2, typeI), emptyList()));
    }

    private void initCriterias() {
        criteria = getCriteria(0, 10, null);

        compareDataCriteria = new CompareDataCriteria(OLD_ID, NEW_ID);

        deletedCompareDataCriteria = new CompareDataCriteria(OLD_ID, NEW_ID);
        deletedCompareDataCriteria.setDiffStatus(DiffStatusEnum.DELETED);
        deletedCompareDataCriteria.setCountOnly(true);
    }

    private void initAttributes() {
        id = Structure.Attribute.buildPrimary("ID", "id", FieldType.INTEGER, "id");
        code = Structure.Attribute.buildPrimary("CODE", "code", FieldType.STRING, "code");
        common = Structure.Attribute.build("COMMON", "common", FieldType.STRING, false, "common");
        descr = Structure.Attribute.build("DESCR", "descr", FieldType.STRING, false, "descr");
        name = Structure.Attribute.build("NAME", "name", FieldType.STRING, false, "name");
        upd1 = Structure.Attribute.build("UPD", "upd1", FieldType.STRING, false, "upd");
        upd2 = Structure.Attribute.build("UPD", "upd2", FieldType.STRING, false, "upd");
        typeS = Structure.Attribute.build("TYPE", "type", FieldType.STRING, false, "type");
        typeI = Structure.Attribute.build("TYPE", "type", FieldType.INTEGER, false, "type");
    }

    private void initFields() {
        idField = new CommonField(id.getCode());
        codeField = new CommonField(code.getCode());
        commonField = new CommonField(common.getCode());
    }

    private void prepareRefBookDataDiff() {
        List<DiffRowValue> expectedDiffRowValues = new ArrayList<>();
        expectedDiffRowValues.add(new DiffRowValue(
                asList(
                        new DiffFieldValue<>(idField, BigInteger.valueOf(1), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(codeField, "001", null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(commonField, "c1", null, DiffStatusEnum.DELETED)),
                DiffStatusEnum.DELETED));
        expectedDiffRowValues.add(new DiffRowValue(
                asList(
                        new DiffFieldValue<>(idField, null, BigInteger.valueOf(4), DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(codeField, null, "004", DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(commonField, null, "c4", DiffStatusEnum.INSERTED)),
                DiffStatusEnum.INSERTED));
        expectedDiffRowValues.add(new DiffRowValue(
                asList(
                        new DiffFieldValue<>(idField, null, BigInteger.valueOf(3), null),
                        new DiffFieldValue<>(codeField, null, "003", null),
                        new DiffFieldValue<>(commonField, "c3", "c3_1", DiffStatusEnum.UPDATED)),
                DiffStatusEnum.UPDATED));

        RefBookDataDiff refBookDataDiff = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(expectedDiffRowValues.size(), expectedDiffRowValues, criteria)),
                singletonList(descr.getCode()),
                singletonList(name.getCode()),
                asList(upd1.getCode(), typeI.getCode())
        );

        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(refBookDataDiff);
    }

    private void prepareRefBookDeletedDataDiff() {
        RefBookDataDiff deletedDataDiff = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(1, singletonList(new DiffRowValue(
                        asList(
                                new DiffFieldValue<>(idField, BigInteger.valueOf(1), null, DiffStatusEnum.DELETED),
                                new DiffFieldValue<>(codeField, "001", null, DiffStatusEnum.DELETED),
                                new DiffFieldValue<>(commonField, "c1", null, DiffStatusEnum.DELETED)),
                        DiffStatusEnum.DELETED)), criteria)),
                singletonList(descr.getCode()),
                singletonList(name.getCode()),
                asList(upd1.getCode(), typeI.getCode())
        );

        when(compareService.compareData(deletedCompareDataCriteria)).thenReturn(deletedDataDiff);
    }

    private void prepareOldVersionData() {
        CollectionPage<RowValue> oldVersionRows = new CollectionPage<>(3, asList(
                new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(1)),
                        new StringFieldValue(code.getCode(), "001"),
                        new StringFieldValue(common.getCode(), "c1"),
                        new StringFieldValue(descr.getCode(), "descr1"),
                        new StringFieldValue(upd1.getCode(), "u1"),
                        new StringFieldValue(typeS.getCode(), "1")
                ),
                new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(2)),
                        new StringFieldValue(code.getCode(), "002"),
                        new StringFieldValue(common.getCode(), "c2"),
                        new StringFieldValue(descr.getCode(), "descr2"),
                        new StringFieldValue(upd1.getCode(), "u2"),
                        new StringFieldValue(typeS.getCode(), "2")
                ),
                new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(3)),
                        new StringFieldValue(code.getCode(), "003"),
                        new StringFieldValue(common.getCode(), "c3"),
                        new StringFieldValue(descr.getCode(), "descr3"),
                        new StringFieldValue(upd1.getCode(), "u3"),
                        new StringFieldValue(typeS.getCode(), "3")
                )
        ), criteria);

        when(versionService.search(eq(OLD_ID), any(SearchDataCriteria.class))).thenReturn(new RowValuePage(oldVersionRows));
    }

    private void prepareNewVersionData() {
        CollectionPage<RowValue> newVersionRows = new CollectionPage<>(3, asList(
                new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(2)),
                        new StringFieldValue(code.getCode(), "002"),
                        new StringFieldValue(common.getCode(), "c2"),
                        new StringFieldValue(name.getCode(), "name2"),
                        new StringFieldValue(upd1.getCode(), "u2"),
                        new IntegerFieldValue(typeS.getCode(), BigInteger.valueOf(2))
                ),
                new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(3)),
                        new StringFieldValue(code.getCode(), "003"),
                        new StringFieldValue(common.getCode(), "c3_1"),
                        new StringFieldValue(name.getCode(), "name3"),
                        new StringFieldValue(upd1.getCode(), "u3_1"),
                        new IntegerFieldValue(typeS.getCode(), BigInteger.valueOf(3))
                ), new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(4)),
                        new StringFieldValue(code.getCode(), "004"),
                        new StringFieldValue(common.getCode(), "c4"),
                        new StringFieldValue(name.getCode(), "name4"),
                        new StringFieldValue(upd1.getCode(), "u4"),
                        new IntegerFieldValue(typeS.getCode(), BigInteger.valueOf(4))
                )
        ), criteria);

        when(versionService.search(eq(NEW_ID), any(SearchDataCriteria.class))).thenReturn(new RowValuePage(newVersionRows));
    }

    private Criteria getCriteria(int pageNumber, int pageSize, Integer count) {
        Criteria criteria = new Criteria();
        criteria.setPage(pageNumber);
        criteria.setSize(pageSize);
        criteria.setCount(count);
        return criteria;
    }

    private CompareDataCriteria getCompareDataCriteria(Integer oldId, Integer newId, int pageNumber, int pageSize) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(oldId, newId);
        compareDataCriteria.setPageNumber(pageNumber);
        compareDataCriteria.setPageSize(pageSize);
        return compareDataCriteria;
    }

    private SearchDataCriteria getSearchDataCriteria(int pageNumber, int pageSize) {
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setPageNumber(pageNumber);
        searchDataCriteria.setPageSize(pageSize);
        return searchDataCriteria;
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
        ComparableField idField = new ComparableField(id.getCode(), id.getName(), null);
        ComparableField codeField = new ComparableField(code.getCode(), code.getName(), null);
        ComparableField commonField = new ComparableField(common.getCode(), common.getName(), null);
        ComparableField nameField = new ComparableField(name.getCode(), name.getName(), DiffStatusEnum.INSERTED);
        ComparableField updField = new ComparableField(upd2.getCode(), upd2.getName(), DiffStatusEnum.UPDATED);
        ComparableField typeField = new ComparableField(typeI.getCode(), typeI.getName(), DiffStatusEnum.UPDATED);
        ComparableField descrField = new ComparableField(descr.getCode(), descr.getName(), DiffStatusEnum.DELETED);
        Page<ComparableRow> expectedCommonDataDiff = new RestPage<>(asList(
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(2), BigInteger.valueOf(2)),
                        new ComparableFieldValue(codeField, "002", "002"),
                        new ComparableFieldValue(commonField, "c2", "c2"),
                        new ComparableFieldValue(nameField, null, "name2"),
                        new ComparableFieldValue(updField, "u2", "u2"),
                        new ComparableFieldValue(typeField, "2", BigInteger.valueOf(2)),
                        new ComparableFieldValue(descrField, "descr2", null)
                ),
                        null),
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(3), BigInteger.valueOf(3)),
                        new ComparableFieldValue(codeField, "003", "003"),
                        new ComparableFieldValue(commonField, "c3", "c3_1"),
                        new ComparableFieldValue(nameField, null, "name3"),
                        new ComparableFieldValue(updField, "u3", "u3_1"),
                        new ComparableFieldValue(typeField, "3", BigInteger.valueOf(3)),
                        new ComparableFieldValue(descrField, "descr3", null)
                ),
                        DiffStatusEnum.UPDATED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, null, BigInteger.valueOf(4)),
                        new ComparableFieldValue(codeField, null, "004"),
                        new ComparableFieldValue(commonField, null, "c4"),
                        new ComparableFieldValue(nameField, null, "name4"),
                        new ComparableFieldValue(updField, null, "u4"),
                        new ComparableFieldValue(typeField, null, BigInteger.valueOf(4)),
                        new ComparableFieldValue(descrField, null, null)
                ),
                        DiffStatusEnum.INSERTED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(1), null),
                        new ComparableFieldValue(codeField, "001", null),
                        new ComparableFieldValue(commonField, "c1", null),
                        new ComparableFieldValue(updField, "u1", null),
                        new ComparableFieldValue(typeField, "1", null),
                        new ComparableFieldValue(descrField, "descr1", null),
                        new ComparableFieldValue(descrField, "descr1", null)
                ),
                        DiffStatusEnum.DELETED)
        ));

        CollectionPage<RowValue> deletedRows = new CollectionPage<>(1, singletonList(
                new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(1)),
                        new StringFieldValue(code.getCode(), "001"),
                        new StringFieldValue(common.getCode(), "c1"),
                        new StringFieldValue(descr.getCode(), "descr1"),
                        new StringFieldValue(upd1.getCode(), "u1"),
                        new StringFieldValue(typeS.getCode(), "1")
                )), criteria);
        when(versionService.search(eq(OLD_ID), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(0, 7))))).thenReturn(new RowValuePage(deletedRows));

        Page<ComparableRow> actualCommonDataDiff = compareDataController.getCommonDataDiff(compareDataCriteria);
        assertComparableRowsEquals(expectedCommonDataDiff, actualCommonDataDiff);
    }

    /*
     * test getting rows for both versions with applied diff in one structure for merged display
     * two fully filled pages: first page with new (inserted) rows, second - with old (deleted) rows
     * count of new and old data is equal to page size
     */
    @Test
    public void testCommonDataDiffExactDivisionOnPagesWithNewAndDeletedRows() {
        int DEF_PAGE_SIZE = 4;
        ComparableField idFieldComp = new ComparableField(id.getCode(), id.getName(), null);
        ComparableField commonFieldComp = new ComparableField(common.getCode(), common.getName(), null);

        when(versionService.getStructure(OLD_ID_1)).thenReturn(new Structure(asList(id, common), emptyList()));
        when(versionService.getStructure(NEW_ID_1)).thenReturn(new Structure(asList(id, common), emptyList()));

        List<RowValue> oldVersionRows = Stream.of(5, 6, 7, 8)
                .map(index -> new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "old" + index)
                ))
                .collect(Collectors.toList());

        List<RowValue> newVersionRows = Stream.of(1, 2, 3, 4)
                .map(index -> new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "new" + index)
                ))
                .collect(Collectors.toList());

        when(versionService
                .search(eq(NEW_ID_1), any(SearchDataCriteria.class)))
                .thenReturn(new RowValuePage(new CollectionPage<>(4, emptyList(), new Criteria())));
        when(versionService
                .search(eq(NEW_ID_1), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(0, DEF_PAGE_SIZE)))))
                .thenReturn(new RowValuePage(new CollectionPage<>(5, newVersionRows.subList(0, 4), getCriteria(0, DEF_PAGE_SIZE, 5))));

        when(versionService
                .search(eq(OLD_ID_1), any(SearchDataCriteria.class)))
                .thenReturn(new RowValuePage(new CollectionPage<>(4, emptyList(), new Criteria())));
        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(0, DEF_PAGE_SIZE)))))
                .thenReturn(new RowValuePage(new CollectionPage<>(4, oldVersionRows, getCriteria(0, 4, 4))));

//        test first page
        Page<ComparableRow> expectedCommonDataDiffPage1 = new RestPage<>(Stream.of(1, 2, 3, 4)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, null, BigInteger.valueOf(index)),
                                new ComparableFieldValue(commonFieldComp, null, "new" + index)
                        ), DiffStatusEnum.INSERTED)
                )
                .collect(Collectors.toList()));

        List<DiffRowValue> diffRowValuesListPage1 = Stream.of(1, 2, 3, 4)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, null, BigInteger.valueOf(index), DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(commonField, null, "new" + index, DiffStatusEnum.INSERTED)
                ), DiffStatusEnum.INSERTED))
                .collect(Collectors.toList());

        RefBookDataDiff refBookDataDiffPage1 = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(diffRowValuesListPage1.size(), diffRowValuesListPage1, getCriteria(0, DEF_PAGE_SIZE, 8))),
                emptyList(),
                emptyList(),
                emptyList()
        );
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(refBookDataDiffPage1);

        Page<ComparableRow> actualCommonDataDiffPage1 = compareDataController.getCommonDataDiff(getCompareDataCriteria(OLD_ID_1, NEW_ID_1, 0, DEF_PAGE_SIZE));
        assertComparableRowsEquals(expectedCommonDataDiffPage1, actualCommonDataDiffPage1);

//        test second page
        Page<ComparableRow> expectedCommonDataDiffPage2 = new RestPage<>(Stream.of(5, 6, 7, 8)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, BigInteger.valueOf(index), null),
                                new ComparableFieldValue(commonFieldComp, "old" + index, null)
                        ), DiffStatusEnum.DELETED)
                )
                .collect(Collectors.toList()));

        List<DiffRowValue> diffRowValuesListPage2 = Stream.of(5, 6, 7, 8)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, BigInteger.valueOf(index), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(commonField, "old" + index, null, DiffStatusEnum.DELETED)
                ), DiffStatusEnum.DELETED))
                .collect(Collectors.toList());

        RefBookDataDiff refBookDataDiffPage2 = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(diffRowValuesListPage2.size(), diffRowValuesListPage2, getCriteria(1, DEF_PAGE_SIZE, 8))),
                emptyList(),
                emptyList(),
                emptyList()
        );
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(refBookDataDiffPage2);

        Page<ComparableRow> actualCommonDataDiffPage2 = compareDataController.getCommonDataDiff(getCompareDataCriteria(OLD_ID_1, NEW_ID_1, 1, DEF_PAGE_SIZE));
        assertComparableRowsEquals(expectedCommonDataDiffPage2, actualCommonDataDiffPage2);
    }

    /*
     * test getting rows for both versions with applied diff in one structure for merged display
     * there are several pages with new (inserted) and old (deleted) rows that are mixed on second page
     */
    @Test
    public void testCommonDataDiffSeveralPagesWithNewAndDeletedRows() {
        int DEF_PAGE_SIZE = 4;
        ComparableField idFieldComp = new ComparableField(id.getCode(), id.getName(), null);
        ComparableField commonFieldComp = new ComparableField(common.getCode(), common.getName(), null);

        when(versionService.getStructure(OLD_ID_1)).thenReturn(new Structure(asList(id, common), emptyList()));
        when(versionService.getStructure(NEW_ID_1)).thenReturn(new Structure(asList(id, common), emptyList()));

        List<RowValue> oldVersionRows = Stream.of(6, 7, 8, 9, 10, 11, 12, 13)
                .map(index -> new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "old" + index)
                ))
                .collect(Collectors.toList());

        List<RowValue> newVersionRows = Stream.of(1, 2, 3, 4, 5)
                .map(index -> new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(index)),
                        new StringFieldValue(common.getCode(), "new" + index)
                ))
                .collect(Collectors.toList());

        when(versionService
                .search(eq(NEW_ID_1), any(SearchDataCriteria.class)))
                .thenReturn(new RowValuePage(new CollectionPage<>(5, emptyList(), new Criteria())));
        when(versionService
                .search(eq(NEW_ID_1), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(0, DEF_PAGE_SIZE)))))
                .thenReturn(new RowValuePage(new CollectionPage<>(5, newVersionRows.subList(0, 4), getCriteria(0, DEF_PAGE_SIZE, 5))));
        when(versionService
                .search(eq(NEW_ID_1), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(1, DEF_PAGE_SIZE)))))
                .thenReturn(new RowValuePage(new CollectionPage<>(5, newVersionRows.subList(4, 5), getCriteria(1, DEF_PAGE_SIZE, 5))));

        when(versionService
                .search(eq(OLD_ID_1), any(SearchDataCriteria.class)))
                .thenReturn(new RowValuePage(new CollectionPage<>(8, emptyList(), new Criteria())));
        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(0, 3)))))
                .thenReturn(new RowValuePage(new CollectionPage<>(3, oldVersionRows.subList(0, 3), getCriteria(0, 3, 8))));
        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(0, 7)))))
                .thenReturn(new RowValuePage(new CollectionPage<>(7, oldVersionRows.subList(0, 7), getCriteria(0, 7, 8))));
        when(versionService
                .search(eq(OLD_ID_1), argThat(new SearchDataCriteriaMatcher(getSearchDataCriteria(0, 11)))))
                .thenReturn(new RowValuePage(new CollectionPage<>(8, oldVersionRows, getCriteria(0, 11, 8))));

//        test first page
        Page<ComparableRow> expectedCommonDataDiffPage1 = new RestPage<>(Stream.of(1, 2, 3, 4)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, null, BigInteger.valueOf(index)),
                                new ComparableFieldValue(commonFieldComp, null, "new" + index)
                        ), DiffStatusEnum.INSERTED)
                )
                .collect(Collectors.toList()));

        List<DiffRowValue> diffRowValuesListPage1 = Stream.of(1, 2, 3, 4)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, null, BigInteger.valueOf(index), DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(commonField, null, "new" + index, DiffStatusEnum.INSERTED)
                ), DiffStatusEnum.INSERTED))
                .collect(Collectors.toList());

        RefBookDataDiff refBookDataDiffPage1 = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(diffRowValuesListPage1.size(), diffRowValuesListPage1, getCriteria(0, DEF_PAGE_SIZE, 13))),
                emptyList(),
                emptyList(),
                emptyList()
        );
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(refBookDataDiffPage1);

        Page<ComparableRow> actualCommonDataDiffPage1 = compareDataController.getCommonDataDiff(getCompareDataCriteria(OLD_ID_1, NEW_ID_1, 0, DEF_PAGE_SIZE));
        assertComparableRowsEquals(expectedCommonDataDiffPage1, actualCommonDataDiffPage1);

//        test second page
        List<ComparableRow> expectedComparableRowsListPage2 = new ArrayList<>();
        expectedComparableRowsListPage2.addAll(Stream.of(5)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, null, BigInteger.valueOf(index)),
                                new ComparableFieldValue(commonFieldComp, null, "new" + index)
                        ), DiffStatusEnum.INSERTED)
                )
                .collect(Collectors.toList()));
        expectedComparableRowsListPage2.addAll(Stream.of(6, 7, 8)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, BigInteger.valueOf(index), null),
                                new ComparableFieldValue(commonFieldComp, "old" + index, null)
                        ), DiffStatusEnum.DELETED)
                )
                .collect(Collectors.toList()));
        Page<ComparableRow> expectedCommonDataDiffPage2 = new RestPage<>(expectedComparableRowsListPage2);

        List<DiffRowValue> diffRowValuesListPage2 = new ArrayList<>();
        diffRowValuesListPage2.addAll(Stream.of(5)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, null, BigInteger.valueOf(index), DiffStatusEnum.INSERTED),
                        new DiffFieldValue<>(commonField, null, "new" + index, DiffStatusEnum.INSERTED)
                ), DiffStatusEnum.INSERTED))
                .collect(Collectors.toList()));
        diffRowValuesListPage2.addAll(Stream.of(6, 7, 8)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, BigInteger.valueOf(index), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(commonField, "old" + index, null, DiffStatusEnum.DELETED)
                ), DiffStatusEnum.DELETED))
                .collect(Collectors.toList()));

        RefBookDataDiff refBookDataDiffPage2 = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(diffRowValuesListPage2.size(), diffRowValuesListPage2, getCriteria(1, DEF_PAGE_SIZE, 13))),
                emptyList(),
                emptyList(),
                emptyList()
        );
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(refBookDataDiffPage2);

        Page<ComparableRow> actualCommonDataDiffPage2 = compareDataController.getCommonDataDiff(getCompareDataCriteria(OLD_ID_1, NEW_ID_1, 1, DEF_PAGE_SIZE));
        assertComparableRowsEquals(expectedCommonDataDiffPage2, actualCommonDataDiffPage2);

//        test third page
        Page<ComparableRow> expectedCommonDataDiffPage3 = new RestPage<>(Stream.of(9, 10, 11, 12)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, BigInteger.valueOf(index), null),
                                new ComparableFieldValue(commonFieldComp, "old" + index, null)
                        ), DiffStatusEnum.DELETED)
                )
                .collect(Collectors.toList()));

        List<DiffRowValue> diffRowValuesListPage3 = Stream.of(9, 10, 11, 12)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, BigInteger.valueOf(index), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(commonField, "old" + index, null, DiffStatusEnum.DELETED)
                ), DiffStatusEnum.DELETED))
                .collect(Collectors.toList());

        RefBookDataDiff refBookDataDiffPage3 = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(diffRowValuesListPage3.size(), diffRowValuesListPage3, getCriteria(2, DEF_PAGE_SIZE, 13))),
                emptyList(),
                emptyList(),
                emptyList()
        );
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(refBookDataDiffPage3);

        Page<ComparableRow> actualCommonDataDiffPage3 = compareDataController.getCommonDataDiff(getCompareDataCriteria(OLD_ID_1, NEW_ID_1, 2, DEF_PAGE_SIZE));
        assertComparableRowsEquals(expectedCommonDataDiffPage3, actualCommonDataDiffPage3);

//        test forth page
        Page<ComparableRow> expectedCommonDataDiffPage4 = new RestPage<>(Stream.of(13)
                .map(index ->
                        new ComparableRow(asList(
                                new ComparableFieldValue(idFieldComp, BigInteger.valueOf(index), null),
                                new ComparableFieldValue(commonFieldComp, "old" + index, null)
                        ), DiffStatusEnum.DELETED)
                )
                .collect(Collectors.toList()));

        List<DiffRowValue> diffRowValuesListPage4 = Stream.of(13)
                .map(index -> new DiffRowValue(asList(
                        new DiffFieldValue<>(idField, BigInteger.valueOf(index), null, DiffStatusEnum.DELETED),
                        new DiffFieldValue<>(commonField, "old" + index, null, DiffStatusEnum.DELETED)
                ), DiffStatusEnum.DELETED))
                .collect(Collectors.toList());

        RefBookDataDiff refBookDataDiffPage4 = new RefBookDataDiff(
                new DiffRowValuePage(new CollectionPage<>(diffRowValuesListPage3.size(), diffRowValuesListPage4, getCriteria(3, DEF_PAGE_SIZE, 13))),
                emptyList(),
                emptyList(),
                emptyList()
        );
        when(compareService.compareData(any(CompareDataCriteria.class))).thenReturn(refBookDataDiffPage4);

        Page<ComparableRow> actualCommonDataDiffPage4 = compareDataController.getCommonDataDiff(getCompareDataCriteria(OLD_ID_1, NEW_ID_1, 3, DEF_PAGE_SIZE));
        assertComparableRowsEquals(expectedCommonDataDiffPage4, actualCommonDataDiffPage4);
    }

    /*
     * test getting rows for old (left) version with applied diff for two-part display
     * deleted rows must be moved to the end of the list (for each page)
     * deleted fields must be moved to the end of the fields list
     */
    @Test
    public void testGetOldWithDiff() {
        ComparableField idField = new ComparableField(id.getCode(), id.getName(), null);
        ComparableField codeField = new ComparableField(code.getCode(), code.getName(), null);
        ComparableField commonField = new ComparableField(common.getCode(), common.getName(), null);
        ComparableField descrField = new ComparableField(descr.getCode(), descr.getName(), DiffStatusEnum.DELETED);
        ComparableField updField = new ComparableField(upd1.getCode(), upd1.getName(), DiffStatusEnum.UPDATED);
        ComparableField typeField = new ComparableField(typeS.getCode(), typeS.getName(), DiffStatusEnum.UPDATED);
        Page<ComparableRow> expectedOldRowsWithDiff = new RestPage<>(asList(
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(1), null),
                        new ComparableFieldValue(codeField, "001", null),
                        new ComparableFieldValue(commonField, "c1", null),
                        new ComparableFieldValue(updField, "u1", null),
                        new ComparableFieldValue(typeField, "1", null),
                        new ComparableFieldValue(descrField, "descr1", null)
                ),
                        DiffStatusEnum.DELETED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(2), BigInteger.valueOf(2)),
                        new ComparableFieldValue(codeField, "002", "002"),
                        new ComparableFieldValue(commonField, "c2", "c2"),
                        new ComparableFieldValue(updField, "u2", "u2"),
                        new ComparableFieldValue(typeField, "2", BigInteger.valueOf(2)),
                        new ComparableFieldValue(descrField, "descr2", null)
                ),
                        null),
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(3), BigInteger.valueOf(3)),
                        new ComparableFieldValue(codeField, "003", "003"),
                        new ComparableFieldValue(commonField, "c3", "c3_1"),
                        new ComparableFieldValue(updField, "u3", null),
                        new ComparableFieldValue(typeField, "3", null),
                        new ComparableFieldValue(descrField, "descr3", null)
                ),
                        DiffStatusEnum.UPDATED)
        ));
        Page<ComparableRow> actualOldRowsWithDiff = compareDataController.getOldWithDiff(compareDataCriteria);
        assertComparableRowsEquals(expectedOldRowsWithDiff, actualOldRowsWithDiff);
    }

    /*
     * test getting rows for new (right) version with applied diff for two-part display
     */
    @Test
    public void testGetNewWithDiff() {
        ComparableField idField = new ComparableField(id.getCode(), id.getName(), null);
        ComparableField codeField = new ComparableField(code.getCode(), code.getName(), null);
        ComparableField commonField = new ComparableField(common.getCode(), common.getName(), null);
        ComparableField nameField = new ComparableField(name.getCode(), name.getName(), DiffStatusEnum.INSERTED);
        ComparableField updField = new ComparableField(upd2.getCode(), upd2.getName(), DiffStatusEnum.UPDATED);
        ComparableField typeField = new ComparableField(typeI.getCode(), typeI.getName(), DiffStatusEnum.UPDATED);
        Page<ComparableRow> expectedNewRowsWithDiff = new RestPage<>(asList(
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(2), BigInteger.valueOf(2)),
                        new ComparableFieldValue(codeField, "002", "002"),
                        new ComparableFieldValue(commonField, "c2", "c2"),
                        new ComparableFieldValue(nameField, null, "name2"),
                        new ComparableFieldValue(updField, "u2", "u2"),
                        new ComparableFieldValue(typeField, "2", BigInteger.valueOf(2))
                ),
                        null),
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, BigInteger.valueOf(3), BigInteger.valueOf(3)),
                        new ComparableFieldValue(codeField, "003", "003"),
                        new ComparableFieldValue(commonField, "c3", "c3_1"),
                        new ComparableFieldValue(nameField, null, "name3"),
                        new ComparableFieldValue(updField, null, "u3_1"),
                        new ComparableFieldValue(typeField, null, BigInteger.valueOf(3))
                ),
                        DiffStatusEnum.UPDATED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idField, null, BigInteger.valueOf(4)),
                        new ComparableFieldValue(codeField, null, "004"),
                        new ComparableFieldValue(commonField, null, "c4"),
                        new ComparableFieldValue(nameField, null, "name4"),
                        new ComparableFieldValue(updField, null, "u4"),
                        new ComparableFieldValue(typeField, null, BigInteger.valueOf(4))
                ),
                        DiffStatusEnum.INSERTED)
        ));
        Page<ComparableRow> actualNewRowsWithDiff = compareDataController.getNewWithDiff(compareDataCriteria);
        assertComparableRowsEquals(expectedNewRowsWithDiff, actualNewRowsWithDiff);
    }

    /*
     * assert that right rows are in the right order
     */
    private void assertComparableRowsEquals(Page<ComparableRow> expectedRowsWithDiff, Page<ComparableRow> actualRowsWithDiff) {
        assertEquals(expectedRowsWithDiff.getContent().size(), actualRowsWithDiff.getContent().size());

        for (int i = 0; i < expectedRowsWithDiff.getContent().size(); i++) {
            ComparableRow expectedRow = expectedRowsWithDiff.getContent().get(i);
            ComparableRow actualRow = actualRowsWithDiff.getContent().get(i);
            assertTrue(statusEquals(actualRow.getStatus(), expectedRow.getStatus())
                    && actualRow.getFieldValues().size() == expectedRow.getFieldValues().size()
                    && actualRow.getFieldValues().containsAll(expectedRow.getFieldValues()));
        }
    }

    private boolean statusEquals(DiffStatusEnum status1, DiffStatusEnum status2) {
        return status1 == null
                ? status2 == null
                : status1.equals(status2);
    }

    private static class SearchDataCriteriaMatcher extends ArgumentMatcher<SearchDataCriteria> {

        private SearchDataCriteria expected;

        SearchDataCriteriaMatcher(SearchDataCriteria criteria) {
            this.expected = criteria;
        }

        @Override
        public boolean matches(Object actual) {
            return actual instanceof SearchDataCriteria &&
                    expected.getPageSize() == ((SearchDataCriteria) actual).getPageSize() &&
                    expected.getPageNumber() == ((SearchDataCriteria) actual).getPageNumber();
        }
    }

}
