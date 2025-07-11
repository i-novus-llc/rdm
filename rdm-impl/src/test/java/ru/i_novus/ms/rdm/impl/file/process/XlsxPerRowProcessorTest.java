package ru.i_novus.ms.rdm.impl.file.process;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;
import ru.i_novus.ms.rdm.impl.util.mappers.StructureRowMapper;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by tnurdinov on 06.07.2018.
 */
public class XlsxPerRowProcessorTest {

    @Test
    public void testSimpleProcessFile() throws Exception {
        List<Map<String, Object>> expected = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date =  LocalDate.parse("01.01.2011", formatter);
        expected.add(new HashMap<>() {{
            put("Kod", BigInteger.valueOf(0));
            put("Opis", "Не требует изготовления полиса");
            put("DATEBEG", date);
            put("DATEEND", null);
        }});
        expected.add(new HashMap<>() {{
            put("Kod", BigInteger.valueOf(1));
            put("Opis", "Бумажный полис ОМС");
            put("DATEBEG", date);
            put("DATEEND", null);
        }});
        expected.add(new HashMap<>() {{
            put("Kod", BigInteger.valueOf(2));
            put("Opis", "Электронный полис ОМС");
            put("DATEBEG", date);
            put("DATEEND", null);
        }});
        expected.add(new HashMap<>() {{
            put("Kod", BigInteger.valueOf(3));
            put("Opis", "Электронный полис ОМС в составе УЭК");
            put("DATEBEG", date);
            put("DATEEND", null);
        }});
        RowsProcessor testRowsProcessor = getTestRowsProcessor(expected);
        try (FilePerRowProcessor processor = new XlsxPerRowProcessor(new StructureRowMapper(createTestStructure(), null), testRowsProcessor)) {
            Result result = processor.process(() -> XlsxPerRowProcessorTest.class.getResourceAsStream("/R002.xlsx"));
            Assert.assertEquals(4, result.getAllCount());
            Assert.assertEquals(4, result.getSuccessCount());
            Assert.assertNull(result.getErrors());
        }
    }

    private Structure createTestStructure() {

        return new Structure(
                Arrays.asList(
                        Structure.Attribute.build("Kod", "Kod", FieldType.INTEGER, "Kod"),
                        Structure.Attribute.build("Opis", "Opis", FieldType.STRING, "Opis"),
                        Structure.Attribute.build("DATEBEG", "DATEBEG", FieldType.DATE, "DATEBEG")
                ),
                null
        );
    }

    private RowsProcessor getTestRowsProcessor(List<Map<String, Object>> expected) {
        return new RowsProcessor() {

            private int successCount = 0;

            private int allCount = 0;

            @Override
            public Result append(Row row) {
                Assert.assertTrue(row.getData().containsKey("Kod"));
                Assert.assertTrue(row.getData().containsKey("Opis"));
                Assert.assertTrue(row.getData().containsKey("DATEBEG"));
                Assert.assertTrue(row.getData().containsKey("DATEEND"));
                successCount++;
                allCount++;
                expected.remove(row.getData());
                return new Result(1, 1, null);
            }

            @Override
            public Result process() {
                Assert.assertTrue(expected.isEmpty());
                return new Result(successCount, allCount, null);
            }
        };
    }

}
