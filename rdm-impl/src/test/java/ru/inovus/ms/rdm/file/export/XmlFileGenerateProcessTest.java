package ru.inovus.ms.rdm.file.export;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.custommonkey.xmlunit.XMLUnit.compareXML;
import static org.junit.Assert.assertTrue;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;
import static ru.inovus.ms.rdm.util.TimeUtils.parseLocalDate;

public class XmlFileGenerateProcessTest {

    @Test
    public void testXmlFileGenerate() throws IOException, SAXException {

        RefBookVersion version = new RefBookVersion();
        version.setCode("код справочника");
        version.setStructure(createFullTestStructure());
        version.setPassport(new LinkedHashMap<>() {{
            put("name", "наименование справочника");
            put("shortName", "краткое наим-ие");
            put("description", "описание");
        }});

        List<Row> rows = createRowsValues()
                .stream()
                .map(Row::new)
                .collect(toList());

        Reader expectedXml = new InputStreamReader(getClass().getResourceAsStream("/file/uploadFile.xml"));
        Reader actualXml;
        try (PerRowFileGenerator xmlFileGenerator = new XmlFileGenerator(rows.iterator(), version);
             ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            xmlFileGenerator.generate(os);
            actualXml = new StringReader(os.toString());
        }

        XMLUnit.setIgnoreWhitespace(true);
        assertTrue(compareXML(expectedXml, actualXml).identical());
    }

    public static Structure createFullTestStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.build("string", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build("integer", "integer", FieldType.INTEGER, "число"),
                        Structure.Attribute.build("date", "date", FieldType.DATE, "дата"),
                        Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, "булево"),
                        Structure.Attribute.build("float", "float", FieldType.FLOAT, "дробное"),
                        Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(new Structure.Reference("reference", -1, "count", toPlaceholder("count")))
        );
    }

    public static List<Map<String, Object>> createRowsValues() {
        List<Map<String, Object>> rowValues = new ArrayList<>();
        rowValues.add(new LinkedHashMap<>() {{
            put("reference", "2");
            put("date", parseLocalDate("02.02.2002"));
            put("boolean", true);
            put("string", "string2");
            put("integer", BigInteger.valueOf(2));
            put("float", BigDecimal.valueOf(2.2));
        }});
        rowValues.add(new LinkedHashMap<>() {{
            put("reference", "5");
            put("date", parseLocalDate("05.05.2005"));
            put("boolean", false);
            put("string", "string5");
            put("integer", BigInteger.valueOf(5));
            put("float", BigDecimal.valueOf(5.5));
        }});
        return rowValues;
    }

}