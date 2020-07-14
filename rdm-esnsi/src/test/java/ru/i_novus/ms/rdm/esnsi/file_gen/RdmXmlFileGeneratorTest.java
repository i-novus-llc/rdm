package ru.i_novus.ms.rdm.esnsi.file_gen;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.BiFunction;

@RunWith(JUnit4.class)
public class RdmXmlFileGeneratorTest {

    @Test
    public void test() throws XMLStreamException {
        RefBookMetadata metadata = new RefBookMetadata() {
            @Override
            public String code() {
                return "APP001";
            }

            @Override
            public String name() {
                return "Справочник приложений";
            }

            @Override
            public String shortName() {
                return "Приложения1";
            }

            @Override
            public String description() {
                return "Системный справочник приложений";
            }
        };
        RefBookStructure structure = new RefBookStructure() {
            @Override
            public Collection<Attribute> attributes() {
                List<Attribute> attributes = new ArrayList<>();
                attributes.add(getAttr("code", "Код", FieldType.STRING, "code", true));
                attributes.add(getAttr("name", "Наименование", FieldType.STRING, "name", false));
                attributes.add(getAttr("system", "Система", FieldType.REFERENCE, "", false));
                attributes.add(getAttr("oauth", "Протокол OAuth2", FieldType.BOOLEAN, "", false));
                return attributes;
            }

            @Override
            public Collection<Reference> references() {
                return Collections.singletonList(new Ref(Map.of("attribute", "system", "referenceCode", "SYS001", "displayExpression", "${code}")));
            }
        };
        Map<String, List<AttributeValidation>> validations = Collections.emptyMap();
        BiFunction<String, Object, String> dateToRdmStr = (fieldName, val) -> null;
        BiFunction<String, Object, String> refToRdmStr = (fieldName, val) -> val.toString();
        Collection<Map<String, Object>> data = List.of(
            getApp001DataRecord("epmp-bi", "Анализ данных", "IAS", true),
            getApp001DataRecord("config-service", "Сервис настроек", "config", false),
            getApp001DataRecord("integration-service", "Реестр", "integration", false)
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RdmXmlFileGenerator gen = new RdmXmlFileGenerator(out, metadata, structure, validations, dateToRdmStr, refToRdmStr, data.iterator());
        gen.init();
        gen.fetchData();
        gen.end();
        byte[] bytes = out.toByteArray();
        Source control = Input.fromStream(getClass().getClassLoader().getResourceAsStream("APP001.xml")).build();
        Source test = Input.fromByteArray(bytes).build();
        Diff diff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().build();
        Assert.assertFalse(diff.hasDifferences());
    }

    private static Attr getAttr(String code, String name, FieldType type, String description, boolean isPrimary) {
        return new Attr(Map.of("code", code, "name", name, "type", type, "description", description, "isPrimary", isPrimary));
    }

    private static Map<String, Object> getApp001DataRecord(String code, String name, String system, boolean oauth) {
        return Map.of("code", code, "name", name, "system", system, "oauth", oauth);
    }

    private static class Attr implements RefBookStructure.Attribute {
        Map<String, Object> attrs;
        Attr(Map<String, Object> attrs) {this.attrs = attrs;}
        @Override public String code() {return (String) attrs.get("code");}
        @Override public String name() {return (String) attrs.get("name");}
        @Override public String description() {return (String) attrs.get("description");}
        @Override public FieldType type() {return (FieldType) attrs.get("type");}
        @Override public boolean isPrimary() {return (boolean) attrs.get("isPrimary");}
    }

    private static class Ref implements RefBookStructure.Reference {
        Map<String, String> attrs;
        Ref(Map<String, String> attrs) {this.attrs = attrs;}
        @Override public String attribute() {return attrs.get("attribute");}
        @Override public String referenceCode() {return attrs.get("referenceCode");}
        @Override public String displayExpression() {return attrs.get("displayExpression");}
    }

}
