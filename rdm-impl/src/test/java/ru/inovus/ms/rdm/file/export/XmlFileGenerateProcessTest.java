package ru.inovus.ms.rdm.file.export;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.io.*;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.custommonkey.xmlunit.XMLUnit.compareXML;
import static org.junit.Assert.assertTrue;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;

public class XmlFileGenerateProcessTest {

    @Test
    public void testXmlFileGenerate() throws IOException, SAXException {

        RefBookVersion version = new RefBookVersion();
        version.setStructure(createFullTestStructure());
        version.setPassport(new LinkedHashMap<String, String>() {{
            put("name", "наименование справочника");
            put("shortName", "краткое наим-ие");
            put("description", "описание");
        }});

        List<Row> rows = asList(
                new Row(new LinkedHashMap<String, Object>() {{
                    put("reference", "2");
                    put("date", TimeUtils.parseLocalDate("02.02.2002"));
                    put("boolean", true);
                    put("string", "string2");
                    put("integer", BigInteger.valueOf(2));
                    put("float", 2.2);
                }}),
                new Row(new LinkedHashMap<String, Object>() {{
                    put("reference", "5");
                    put("date", TimeUtils.parseLocalDate("05.05.2005"));
                    put("boolean", false);
                    put("string", "string5");
                    put("integer", BigInteger.valueOf(5));
                    put("float", 5.5);
                }})
        );

        Reader expectedXml = new InputStreamReader(getClass().getResourceAsStream("/file/export/export.xml"));
        Reader actualXml;
        try (PerRowFileGenerator xmlFileGenerator = new XmlFileGenerator(rows.iterator(), version);
             ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            xmlFileGenerator.generate(os);
            actualXml = new StringReader(os.toString());
        }

        XMLUnit.setIgnoreWhitespace(true);
        assertTrue(compareXML(expectedXml, actualXml).identical());
    }

    private Structure createFullTestStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.build("string", "string", FieldType.STRING, false, "строка"),
                        Structure.Attribute.build("integer", "integer", FieldType.INTEGER, false, "число"),
                        Structure.Attribute.build("date", "date", FieldType.DATE, false, "дата"),
                        Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, false, "булево"),
                        Structure.Attribute.build("float", "float", FieldType.FLOAT, false, "дробное"),
                        Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, false, "ссылка")
                ),
                singletonList(new Structure.Reference("reference", -1, "count", toPlaceholder("count")))
        );
    }

}