package ru.inovus.ms.rdm.file.export;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Row;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedHashMap;

import static ru.inovus.ms.rdm.file.export.XmlFileGenerateProcessTest.createFullTestStructure;
import static ru.inovus.ms.rdm.util.TimeUtils.parseLocalDate;

//xml file generator for testing
//generates file with extension with ROWS_COUNT different rows
public class FileGeneratorTest {

    private static Integer count;
    private static Row row;
    private static final int ROWS_COUNT = 10000;

    @BeforeClass
    public static void init() {
        count = 0;
        row = new Row(new LinkedHashMap<String, Object>() {{
            put("reference", "ref1");
            put("date", parseLocalDate("02.02.2002"));
            put("boolean", true);
            put("string", "string3");
            put("integer", BigInteger.valueOf(0));
            put("float", 5.5);
        }});
    }

    @Test
    @Ignore
    public void testXmlFileGenerate() throws IOException {

        RefBookVersion version = new RefBookVersion();
        version.setStructure(createFullTestStructure());
        version.setPassport(new LinkedHashMap<String, String>() {{
            put("name", "наименование справочника");
            put("shortName", "краткое наим-ие");
            put("description", "описание");
        }});

        long start = System.currentTimeMillis();
        System.out.println("start: " + start);

        try (PerRowFileGenerator xmlFileGenerator = new XmlFileGenerator(new RowIterator(), version);
             OutputStream os = new BufferedOutputStream(new FileOutputStream("/file/export/" + ROWS_COUNT + ".xml"));) {
            xmlFileGenerator.generate(os);
        }

        long finishGenerateFile = System.currentTimeMillis();
        System.out.println("file: " + finishGenerateFile);

        long total = finishGenerateFile - start;
        System.out.println("total: " + total);
    }

    private class RowIterator implements Iterator<Row> {

        @Override
        public boolean hasNext() {
            return count < ROWS_COUNT;
        }

        @Override
        public Row next() {
            count++;
            System.out.println(count);
            row.getData().replace("integer", count);
            return row;
        }
    }

}
