package ru.i_novus.ms.rdm.impl.file.export;

import com.monitorjbl.xlsx.StreamingReader;
import net.n2oapp.platform.i18n.UserException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableField;
import ru.i_novus.ms.rdm.api.model.compare.ComparableFieldValue;
import ru.i_novus.ms.rdm.api.model.compare.ComparableRow;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.PassportAttributeDiff;
import ru.i_novus.ms.rdm.api.model.diff.PassportDiff;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.version.PassportAttribute;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.i_novus.ms.rdm.impl.repository.PassportAttributeRepository;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.getCellValue;

/**
 * Created by znurgaliev on 22.10.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class XlsxCompareFileGeneratorTest {

    private static int OLD_VERSION_ID = 1;
    private static int NEW_VERSION_ID = 2;

    @Mock
    private CompareService compareService;
    @Mock
    private VersionService versionService;
    @Mock
    private PassportAttributeRepository passportAttributeRepository;

    private XlsxCompareFileGenerator xlsxCompareGenerator;

    @Before
    public void init() {

        xlsxCompareGenerator = new XlsxCompareFileGenerator(
                OLD_VERSION_ID, NEW_VERSION_ID,
                compareService, versionService, passportAttributeRepository
        );

        PassportAttribute delAttr = new PassportAttribute("delAttr", null);
        PassportAttribute updAttr = new PassportAttribute("updAttr", null);
        PassportAttribute insAttr = new PassportAttribute("insAttr", null);
        PassportAttribute noEditAttr = new PassportAttribute("noEditAttr", null);
        when(compareService.comparePassports(OLD_VERSION_ID, NEW_VERSION_ID))
                .thenReturn(new PassportDiff(asList(
                        new PassportAttributeDiff(delAttr, "oldDelValue", null),
                        new PassportAttributeDiff(updAttr, "oldUpdValue", "newUpdValue"),
                        new PassportAttributeDiff(insAttr, null, "newInsValue")
                )));

        when(passportAttributeRepository.findAllByComparableIsTrueOrderByPositionAsc())
                .thenReturn(asList(
                        new PassportAttributeEntity("delAttr", "Удаленный"),
                        new PassportAttributeEntity("updAttr", "Измененный"),
                        new PassportAttributeEntity("insAttr", "Добавленный"),
                        new PassportAttributeEntity("noEditAttr", "Не измененный")
                ));

        Structure.Attribute pKAttribute = Structure.Attribute.build("PK", "PK name", FieldType.STRING, "PK description");
        Structure.Attribute createdAttribute = Structure.Attribute.build("created", "create name", FieldType.STRING, "create description");
        Structure.Attribute updatedOldAttribute = Structure.Attribute.build("string_to_int", "string to int name", FieldType.STRING, "string to int description");
        Structure.Attribute updatedNewAttribute = Structure.Attribute.build("string_to_int", "string to int name", FieldType.INTEGER, "string to int description");
        Structure.Attribute deletedAttribute = Structure.Attribute.build("deleted", "deleted name", FieldType.STRING, "deleted description");
        Structure.Attribute notEditedAttribute = Structure.Attribute.build("not_edited", "not_edited name", FieldType.STRING, "not_edited description");
        when(compareService.compareStructures(OLD_VERSION_ID, NEW_VERSION_ID))
                .thenReturn(new StructureDiff(
                        singletonList(new StructureDiff.AttributeDiff(null, createdAttribute)),
                        singletonList(new StructureDiff.AttributeDiff(updatedOldAttribute, updatedNewAttribute)),
                        singletonList(new StructureDiff.AttributeDiff(deletedAttribute, null))
                ));

        ComparableField pk = new ComparableField("PK", "PK name", null);
        ComparableField create = new ComparableField("created", "create name", DiffStatusEnum.INSERTED);
        ComparableField string_to_int = new ComparableField("string_to_int", "string to int name", DiffStatusEnum.UPDATED);
        ComparableField not_edited = new ComparableField("not_edited", "not_edited name", null);
        ComparableField deleted = new ComparableField("deleted", "deleted name", DiffStatusEnum.DELETED);
        when(compareService.getCommonComparableRows(argThat(new TestCompareDataCriteriaMatcher(new CompareDataCriteria(OLD_VERSION_ID, NEW_VERSION_ID)))))
                .thenReturn(new PageImpl<>(asList(
                        new ComparableRow(asList(
                                new ComparableFieldValue(pk, 2, 2, null),
                                new ComparableFieldValue(create, null, "c2", DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(string_to_int, "2", 2, null),
                                new ComparableFieldValue(not_edited, "ne2", "ne2", null),
                                new ComparableFieldValue(deleted, "d2", null, DiffStatusEnum.DELETED)
                        ), null),
                        new ComparableRow(asList(
                                new ComparableFieldValue(pk, 3, 3, null),
                                new ComparableFieldValue(create, null, "c3", DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(string_to_int, "s3", 3, DiffStatusEnum.UPDATED),
                                new ComparableFieldValue(not_edited, "ne3", "ne3", null),
                                new ComparableFieldValue(deleted, "d3", null, DiffStatusEnum.DELETED)
                        ), DiffStatusEnum.UPDATED),
                        new ComparableRow(asList(
                                new ComparableFieldValue(pk, 4, 4, null),
                                new ComparableFieldValue(create, null, "c4", DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(string_to_int, "4", 4, null),
                                new ComparableFieldValue(not_edited, "e4", "e41", DiffStatusEnum.UPDATED),
                                new ComparableFieldValue(deleted, "d4", null, DiffStatusEnum.DELETED)
                        ), DiffStatusEnum.UPDATED),
                        new ComparableRow(asList(
                                new ComparableFieldValue(pk, null, 5, DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(create, null, "c2", DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(string_to_int, null, 5, DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(not_edited, null, "ne5", DiffStatusEnum.INSERTED),
                                new ComparableFieldValue(deleted, null, null, DiffStatusEnum.DELETED)
                        ), DiffStatusEnum.INSERTED),
                        new ComparableRow(asList(
                                new ComparableFieldValue(pk, 1, null, DiffStatusEnum.DELETED),
                                new ComparableFieldValue(create, null, null, DiffStatusEnum.DELETED),
                                new ComparableFieldValue(string_to_int, "s1", null, DiffStatusEnum.DELETED),
                                new ComparableFieldValue(not_edited, "ne1", null, DiffStatusEnum.DELETED),
                                new ComparableFieldValue(deleted, "d1", null, DiffStatusEnum.DELETED)
                        ), DiffStatusEnum.DELETED)
                )));

        when(compareService.getCommonComparableRows(
                AdditionalMatchers.not(argThat(
                        new TestCompareDataCriteriaMatcher(
                                new CompareDataCriteria(OLD_VERSION_ID, NEW_VERSION_ID)
                        )
                ))
        ))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(compareService.compareData(new CompareDataCriteria(OLD_VERSION_ID, NEW_VERSION_ID)))
                .thenReturn(new RefBookDataDiff(
                        null,
                        singletonList("deleted"),
                        singletonList("created"),
                        singletonList("string_to_int")
                ));

        RefBookVersion oldVersion = new RefBookVersion();
        oldVersion.setId(OLD_VERSION_ID);
        oldVersion.setStructure(new Structure(
                asList(pKAttribute, updatedOldAttribute, notEditedAttribute, deletedAttribute),
                null
        ));
        when(versionService.getById(OLD_VERSION_ID)).thenReturn(oldVersion);

        RefBookVersion newVersion = new RefBookVersion();
        newVersion.setId(NEW_VERSION_ID);
        newVersion.setStructure(new Structure(
                asList(pKAttribute, createdAttribute, updatedNewAttribute, notEditedAttribute),
                null
        ));
        newVersion.setPassport(createPassport());
        when(versionService.getById(NEW_VERSION_ID)).thenReturn(newVersion);
    }

    private Map<String, String> createPassport() {

        final Map<String, String> map = new HashMap<>(3);
        map.put("updAttr", "newUpdValue");
        map.put("insAttr", "newInsValue");
        map.put("noEditAttr", "noEditValue");

        return map;
    }

    @After
    public void closeGenerator() throws IOException {
        xlsxCompareGenerator.close();
    }

    @Test
    public void testGenerate() throws Exception {

        File actualFile = File.createTempFile("compare_with_data", "xlsx");
        try (OutputStream os = new FileOutputStream(actualFile)) {
            xlsxCompareGenerator.generate(os);
        }

        final String expectedFilePath = "/file/export/compare_test/compared_file.xlsx";
        try (Workbook expected = StreamingReader.builder().rowCacheSize(100)
                .open(XlsxCompareFileGeneratorTest.class.getResourceAsStream(expectedFilePath));
             Workbook actual = StreamingReader.builder().rowCacheSize(100).open(actualFile)) {

            assertWorkbookEquals(expected, actual);
        }
    }

    @Test
    public void testNotComparableData() throws IOException {

        when(compareService.compareData(any())).thenThrow(new UserException("comparing is unavailable"));

        File actualFile = File.createTempFile("compare_no_data", "xlsx");
        try (OutputStream os = new FileOutputStream(actualFile)) {
            xlsxCompareGenerator.generate(os);
        }

        final String expectedFilePath = "/file/export/compare_test/compared_no_data.xlsx";
        try (Workbook expected = StreamingReader.builder().rowCacheSize(100)
                .open(XlsxCompareFileGeneratorTest.class.getResourceAsStream(expectedFilePath));
             Workbook actual = StreamingReader.builder().rowCacheSize(100).open(actualFile)) {

            assertWorkbookEquals(expected, actual);
        }
    }

    private void assertWorkbookEquals(Workbook expected, Workbook actual) {

        Iterator<Sheet> actualSheets = actual.iterator();
        for (Sheet expectedSheet : expected) {

            Iterator<Row> actualRows = actualSheets.next().rowIterator();
            for (Row expectedRow : expectedSheet) {

                Iterator<Cell> actualCells = actualRows.next().cellIterator();
                for (Cell expectedCell : expectedRow) {
                    final Cell actualCell = actualCells.next();
                    assertEquals(expectedCell.getCellStyle(), actualCell.getCellStyle());
                    assertEquals(getCellValue(expectedCell), getCellValue(actualCell));
                }
            }
        }
    }

    private static class TestCompareDataCriteriaMatcher implements ArgumentMatcher<CompareDataCriteria> {

        private final CompareDataCriteria expected;

        TestCompareDataCriteriaMatcher(CompareDataCriteria criteria) {
            this.expected = criteria;
        }

        @Override
        public boolean matches(CompareDataCriteria actual) {

            if (actual == null)
                return false;

           return Objects.equals(expected.getOldVersionId(), actual.getOldVersionId()) &&
                    Objects.equals(expected.getNewVersionId(), actual.getNewVersionId()) &&
                    Objects.equals(expected.getPageNumber(), actual.getPageNumber()) &&
                    Objects.equals(expected.getPageSize(), actual.getPageSize()) &&
                    Objects.equals(expected.getPrimaryAttributesFilters(), actual.getPrimaryAttributesFilters()) &&
                    Objects.equals(expected.getCountOnly(), actual.getCountOnly());
        }
    }
}