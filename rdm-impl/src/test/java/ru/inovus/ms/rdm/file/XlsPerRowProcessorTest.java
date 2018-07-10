package ru.inovus.ms.rdm.file;

import org.junit.Assert;
import org.junit.Test;
import ru.inovus.ms.rdm.model.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tnurdinov on 06.07.2018.
 */
public class XlsPerRowProcessorTest {


    @Test
    public void testSimpleProcessFile() throws Exception {
        List<Map<String, Object>> expected = new ArrayList<>();
        expected.add(new HashMap() {{
            put("Kod", "0");
            put("Opis", "Не требует изготовления полиса");
            put("DATEBEG", "01.01.2011");
        }});
        expected.add(new HashMap() {{
            put("Kod", "1");
            put("Opis", "Бумажный полис ОМС");
            put("DATEBEG", "01.01.2011");
        }});
        expected.add(new HashMap() {{
            put("Kod", "2");
            put("Opis", "Электронный полис ОМС");
            put("DATEBEG", "01.01.2011");
        }});
        expected.add(new HashMap() {{
            put("Kod", "3");
            put("Opis", "Электронный полис ОМС в составе УЭК");
            put("DATEBEG", "01.01.2011");
        }});
        RowsProcessor testRowsProcessor = getTestRowsProcessor(expected);
        Result result = new XlsPerRowProcessor(testRowsProcessor).process(() -> XlsPerRowProcessorTest.class.getResourceAsStream("/R002.xlsx"));
        Assert.assertEquals(4, result.getAllCount());
        Assert.assertEquals(4, result.getSuccessCount());
        Assert.assertNull(result.getErrors());

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
                successCount ++;
                allCount ++;
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
