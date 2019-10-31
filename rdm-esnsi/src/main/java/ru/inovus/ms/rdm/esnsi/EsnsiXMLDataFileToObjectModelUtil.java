package ru.inovus.ms.rdm.esnsi;

import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.AttributeType;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.xml.stream.XMLStreamConstants.*;

class EsnsiXMLDataFileToObjectModelUtil {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newFactory();

    private static final String DATA_ELEM = "data";
    private static final String RECORD_ELEM = "record";
    private static final String ATTR_VALUE = "attribute-value";
    private static final String ATTR_REF = "attribute-ref";

    private EsnsiXMLDataFileToObjectModelUtil() {throw new UnsupportedOperationException();}

    static void read(Consumer<Map<String, Object>> consumer, GetClassifierStructureResponseType struct, InputStream inputStream) {
        Map<String, ClassifierAttribute> attributes = indexAttrs(struct);
        String[] rdmFieldCodes = new String[attributes.size()];
        for (ClassifierAttribute attr : attributes.values())
            rdmFieldCodes[attr.getOrder() - 1] = "field_" + attr.getOrder();
        try {
            XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(inputStream);
            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT && reader.getLocalName().equals(DATA_ELEM))
                    break;
            }
            Map<String, Object> row = new HashMap<>();
            ClassifierAttribute currAttr = null;
            String ln;
            mark: while (reader.hasNext()) {
                int next = reader.next();
                switch (next) {
                    case START_ELEMENT:
                        ln = reader.getLocalName();
                        switch (ln) {
                            case RECORD_ELEM:
                                row = new HashMap<>();
                                break;
                            case ATTR_VALUE:
                                currAttr = attributes.get(getAttrValue(reader));
                                break;
                        }
                        break;
                    case END_ELEMENT:
                        ln = reader.getLocalName();
                        switch (ln) {
                            case RECORD_ELEM:
                                consumer.accept(row);
                                row.clear();
                                break;
                            case DATA_ELEM:
                                break mark;
                        }
                        break;
                    case CHARACTERS:
                        if (currAttr == null)
                            throw new IllegalArgumentException("Invalid XML document.");
                        Object obj = EsnsiToObjectConverterUtil.map(currAttr, reader.getText());
                        row.put(rdmFieldCodes[currAttr.getOrder() - 1], obj);
                        break;
                }
            }
            while (reader.hasNext()) reader.next();
            reader.close();
        } catch (XMLStreamException e) {
            throw new RdmException(e);
        }
    }

    private static String getAttrValue(XMLStreamReader r) {
        for (int i = 0; i < r.getAttributeCount(); i++) {
            String attributeName = r.getAttributeLocalName(i);
            if (attributeName.equals(EsnsiXMLDataFileToObjectModelUtil.ATTR_REF))
                return r.getAttributeValue(i);
        }
        return null;
    }

    private static Map<String, ClassifierAttribute> indexAttrs(GetClassifierStructureResponseType struct) {
        return struct.getAttributeList().stream().collect(toMap(ClassifierAttribute::getUid, identity()));
    }

    private static class EsnsiToObjectConverterUtil {

        private static final DateTimeFormatter ESNSI_DATE_FORMAT = DateTimeFormatter.ofPattern("u-MM-dd");

        private static Object map(ClassifierAttribute attr, String val) {
            AttributeType type = attr.getType();
            switch (type) {
                case STRING:
                case TEXT:
                    return val;
                case BOOLEAN:
                    return Boolean.valueOf(val);
                case INTEGER:
                    return Integer.valueOf(val);
                case DECIMAL:
                    return Double.valueOf(val);
                case DATE:
                    return LocalDate.parse(val, ESNSI_DATE_FORMAT);
                case REFERENCE:
                default:
                    throw new UnsupportedOperationException("We're sorry, unsupported yet.");
            }
        }

    }

}
