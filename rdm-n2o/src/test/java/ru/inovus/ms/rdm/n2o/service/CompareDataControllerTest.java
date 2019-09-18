package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.jaxrs.RestPage;
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
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.inovus.ms.rdm.n2o.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.n2o.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.model.compare.ComparableField;
import ru.inovus.ms.rdm.n2o.model.compare.ComparableFieldValue;
import ru.inovus.ms.rdm.n2o.model.compare.ComparableRow;
import ru.inovus.ms.rdm.n2o.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.n2o.service.api.CompareService;
import ru.inovus.ms.rdm.n2o.service.api.VersionService;

import java.math.BigInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
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

    private static Structure.Attribute id;
    private static Structure.Attribute code;
    private static Structure.Attribute common;
    private static Structure.Attribute descr;
    private static Structure.Attribute name;
    private static Structure.Attribute upd1;
    private static Structure.Attribute upd2;
    private static Structure.Attribute typeS;
    private static Structure.Attribute typeI;

    private static ComparableField idFieldComp;
    private static ComparableField codeFieldComp;
    private static ComparableField commonFieldComp;
    private static ComparableField descrFieldComp;
    private static ComparableField nameFieldComp;
    private static ComparableField upd1FieldComp;
    private static ComparableField upd2FieldComp;
    private static ComparableField typeFieldComp;

    @Before
    public void init() {
        initAttributes();
        initFields();

        prepareOldVersionData();
        prepareNewVersionData();
        prepareCommonComparableRows();

        when(versionService.getStructure(OLD_ID)).thenReturn(new Structure(asList(id, code, common, descr, upd1, typeS), emptyList()));
        when(versionService.getStructure(NEW_ID)).thenReturn(new Structure(asList(id, code, common, name, upd2, typeI), emptyList()));
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
        idFieldComp = new ComparableField(id.getCode(), id.getName(), null);
        codeFieldComp = new ComparableField(code.getCode(), code.getName(), null);
        commonFieldComp = new ComparableField(common.getCode(), common.getName(), null);
        descrFieldComp = new ComparableField(descr.getCode(), descr.getName(), DiffStatusEnum.DELETED);
        nameFieldComp = new ComparableField(name.getCode(), name.getName(), DiffStatusEnum.INSERTED);
        upd1FieldComp = new ComparableField(upd1.getCode(), upd1.getName(), DiffStatusEnum.UPDATED);
        upd2FieldComp = new ComparableField(upd2.getCode(), upd2.getName(), DiffStatusEnum.UPDATED);
        typeFieldComp = new ComparableField(typeS.getCode(), typeS.getName(), DiffStatusEnum.UPDATED);
    }

    private void prepareOldVersionData() {
        PageImpl<RefBookRowValue> oldVersionRows = new PageImpl<>( asList(
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(1)),
                        new StringFieldValue(code.getCode(), "001"),
                        new StringFieldValue(common.getCode(), "c1"),
                        new StringFieldValue(descr.getCode(), "descr1"),
                        new StringFieldValue(upd1.getCode(), "u1"),
                        new StringFieldValue(typeS.getCode(), "1")
                ), OLD_ID),
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(2)),
                        new StringFieldValue(code.getCode(), "002"),
                        new StringFieldValue(common.getCode(), "c2"),
                        new StringFieldValue(descr.getCode(), "descr2"),
                        new StringFieldValue(upd1.getCode(), "u2"),
                        new StringFieldValue(typeS.getCode(), "2")
                ), OLD_ID),
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(id.getCode(), BigInteger.valueOf(3)),
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
        PageImpl<RefBookRowValue> newVersionRows = new PageImpl<>( asList(
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

    private void prepareCommonComparableRows() {
        CompareDataCriteria criteria = createRdmDefaultCompareDataCriteria(OLD_ID, NEW_ID);
        Page<ComparableRow> commonComparableRows = new RestPage<>(asList(
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
        ), criteria, 4);
        when(compareService.getCommonComparableRows(argThat(new CompareDataCriteriaMatcher(criteria)))).thenReturn(commonComparableRows);
    }

    /*
     * test getting rows for old (left) version with applied diff for two-part display
     * deleted rows must be moved to the end of the list (for each page)
     * deleted fields must be moved to the end of the fields list
     */
    @Test
    public void testGetOldWithDiff() {
        Page<ComparableRow> expectedOldRowsWithDiff = new RestPage<>(asList(
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(1), null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(codeFieldComp, "001", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(commonFieldComp, "c1", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(descrFieldComp, "descr1", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(upd1FieldComp, "u1", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(typeFieldComp, "1", null, DiffStatusEnum.DELETED)
                ),
                        DiffStatusEnum.DELETED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(2), BigInteger.valueOf(2), null),
                        new ComparableFieldValue(codeFieldComp, "002", "002", null),
                        new ComparableFieldValue(commonFieldComp, "c2", "c2", null),
                        new ComparableFieldValue(descrFieldComp, "descr2", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(upd1FieldComp, "u2", "u2", null),
                        new ComparableFieldValue(typeFieldComp, "2", BigInteger.valueOf(2), null)
                ),
                        null),
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(3), BigInteger.valueOf(3), null),
                        new ComparableFieldValue(codeFieldComp, "003", "003", null),
                        new ComparableFieldValue(commonFieldComp, "c3", "c3_1", DiffStatusEnum.UPDATED),
                        new ComparableFieldValue(descrFieldComp, "descr3", null, DiffStatusEnum.DELETED),
                        new ComparableFieldValue(upd1FieldComp, "u3", "u3_1", DiffStatusEnum.UPDATED),
                        new ComparableFieldValue(typeFieldComp, "3", BigInteger.valueOf(3), null)
                ),
                        DiffStatusEnum.UPDATED)
        ), createRdmDefaultCompareDataCriteria(OLD_ID, NEW_ID), 3);
        Page<ComparableRow> actualOldRowsWithDiff = compareDataController.getOldWithDiff(new CompareDataCriteria(OLD_ID, NEW_ID));
        assertComparableRowsEquals(expectedOldRowsWithDiff, actualOldRowsWithDiff);
    }

    /*
     * test getting rows for new (right) version with applied diff for two-part display
     */
    @Test
    public void testGetNewWithDiff() {
        Page<ComparableRow> expectedNewRowsWithDiff = new RestPage<>(asList(
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(2), BigInteger.valueOf(2), null),
                        new ComparableFieldValue(codeFieldComp, "002", "002", null),
                        new ComparableFieldValue(commonFieldComp, "c2", "c2", null),
                        new ComparableFieldValue(nameFieldComp, null, "name2", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(upd2FieldComp, "u2", "u2", null),
                        new ComparableFieldValue(typeFieldComp, "2", BigInteger.valueOf(2), null)
                ),
                        null),
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, BigInteger.valueOf(3), BigInteger.valueOf(3), null),
                        new ComparableFieldValue(codeFieldComp, "003", "003", null),
                        new ComparableFieldValue(commonFieldComp, "c3", "c3_1", DiffStatusEnum.UPDATED),
                        new ComparableFieldValue(nameFieldComp, null, "name3", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(upd2FieldComp, "u3", "u3_1", DiffStatusEnum.UPDATED),
                        new ComparableFieldValue(typeFieldComp, "3", BigInteger.valueOf(3), null)
                ),
                        DiffStatusEnum.UPDATED),
                new ComparableRow(asList(
                        new ComparableFieldValue(idFieldComp, null, BigInteger.valueOf(4), DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(codeFieldComp, null, "004", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(commonFieldComp, null, "c4", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(nameFieldComp, null, "name4", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(upd2FieldComp, null, "u4", DiffStatusEnum.INSERTED),
                        new ComparableFieldValue(typeFieldComp, null, BigInteger.valueOf(4), DiffStatusEnum.INSERTED)
                ),
                        DiffStatusEnum.INSERTED)
        ), createRdmDefaultCompareDataCriteria(OLD_ID, NEW_ID), 3);
        Page<ComparableRow> actualNewRowsWithDiff = compareDataController.getNewWithDiff(new CompareDataCriteria(OLD_ID, NEW_ID));
        assertComparableRowsEquals(expectedNewRowsWithDiff, actualNewRowsWithDiff);
    }

    private CompareDataCriteria createRdmDefaultCompareDataCriteria(Integer oldId, Integer newId) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(oldId, newId);
        compareDataCriteria.setPageSize(10);
        return compareDataCriteria;
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

    /*
     * suppose that two rdm CompareDataCriteria values are equal for mocking if equal version ids, countOnly flag and diffStatus
     * ignore page size and page number (from Criteria)
     */
    private static class CompareDataCriteriaMatcher implements ArgumentMatcher<CompareDataCriteria> {

        private CompareDataCriteria expected;

        CompareDataCriteriaMatcher(CompareDataCriteria criteria) {
            this.expected = criteria;
        }

        @Override
        public boolean matches(CompareDataCriteria actual) {
            if (expected == null)
                return false;
            return expected.getOldVersionId().equals(actual.getOldVersionId()) &&
                    expected.getNewVersionId().equals(actual.getNewVersionId()) &&
                    expected.getPageNumber() == actual.getPageNumber() &&
                    expected.getPageSize() == actual.getPageSize();
        }
    }

}
