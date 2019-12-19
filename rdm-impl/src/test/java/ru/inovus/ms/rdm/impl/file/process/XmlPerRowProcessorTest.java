package ru.inovus.ms.rdm.impl.file.process;

import org.junit.Before;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Result;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.impl.file.export.XmlFileGenerateProcessTest;
import ru.inovus.ms.rdm.impl.util.mappers.StructureRowMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class XmlPerRowProcessorTest {

    private final static String XML_FILE = "/file/uploadFile.xml";
    private final static String EMPTY_XML_FILE = "/file/uploadEmpty.xml";
    private Structure structure;
    private List<Map<String, Object>> expected;

    @Before
    public void setUp() {
        structure = XmlFileGenerateProcessTest.createFullTestStructure();
        structure.getAttribute("reference").setType(FieldType.STRING);

        expected = XmlFileGenerateProcessTest.createRowsValues();
    }

    @Test
    public void testSimpleProcessFile() {

        try (XmlPerRowProcessor processor =
                     new XmlPerRowProcessor(new StructureRowMapper(structure, null),
                             getTestRowsProcessor(expected))) {
            Result result = processor.process(() -> getClass().getResourceAsStream(XML_FILE));
            assertEquals(2, result.getAllCount());
            assertEquals(2, result.getSuccessCount());
            assertNull(result.getErrors());
        }
    }

    @Test
    public void testProcessEmptyFile() {

        try (XmlPerRowProcessor processor =
                     new XmlPerRowProcessor(new StructureRowMapper(structure, null),
                             getTestRowsProcessor(new ArrayList<>()))) {
            Result result = processor.process(() -> getClass().getResourceAsStream(EMPTY_XML_FILE));
            assertEquals(0, result.getAllCount());
            assertEquals(0, result.getSuccessCount());
            assertNull(result.getErrors());
        }
    }

    @Test
    public void testHasNext() {

        try (XmlPerRowProcessor processor =
                     new XmlPerRowProcessor(new StructureRowMapper(structure, null),
                             getTestRowsProcessor(expected))) {
            processor.setFile(getClass().getResourceAsStream(XML_FILE));
            assertTrue(processor.hasNext());
            processor.next();
            assertTrue(processor.hasNext());
            processor.next();
            assertFalse(processor.hasNext());
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void testNextThrowExceptionForEmptyFile() {

        try (XmlPerRowProcessor processor =
                     new XmlPerRowProcessor(new StructureRowMapper(structure, null),
                             getTestRowsProcessor(new ArrayList<>()))) {
            processor.setFile(getClass().getResourceAsStream(EMPTY_XML_FILE));
            assertFalse(processor.hasNext());
            processor.next();
        }
    }

    private RowsProcessor getTestRowsProcessor(List<Map<String, Object>> expected) {
        return new RowsProcessor() {

            private int successCount = 0;
            private int allCount = 0;

            @Override
            public Result append(Row row) {
                assertActualValuesMap(expected, row.getData());

                successCount++;
                allCount++;
                expected.remove(row.getData());
                return new Result(1, 1, null);
            }

            @Override
            public Result process() {
                assertTrue(expected.isEmpty());
                return new Result(successCount, allCount, null);
            }
        };
    }

    private void assertActualValuesMap(List<Map<String, Object>> expected, Map<String, Object> actual) {
        assertTrue(expected.stream().anyMatch(
                expectedValues -> actual
                        .keySet()
                        .stream()
                        .allMatch(key ->
                                actual.get(key).equals(expectedValues.get(key))
                        )
                )
        );
    }

}