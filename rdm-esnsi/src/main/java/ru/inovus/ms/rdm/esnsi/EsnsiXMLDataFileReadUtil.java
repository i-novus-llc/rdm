package ru.inovus.ms.rdm.esnsi;

import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.xml.stream.XMLStreamConstants.*;

class EsnsiXMLDataFileReadUtil {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newFactory();

    private static final String DATA_ELEM = "data";
    private static final String RECORD_ELEM = "record";
    private static final String ATTR_VALUE = "attribute-value";
    private static final String ATTR_REF = "attribute-ref";

    private EsnsiXMLDataFileReadUtil() {throw new UnsupportedOperationException();}

    static void read(Consumer<Object[]> consumer, GetClassifierStructureResponseType struct, InputStream inputStream) {
        Map<String, ClassifierAttribute> attributes = indexAttrs(struct);
        try {
            XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(inputStream);
            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT && reader.getLocalName().equals(DATA_ELEM))
                    break;
            }
            int i = 0;
            Object[] row = new Object[struct.getAttributeList().size()];
            ClassifierAttribute currAttr = null;
            String ln;
            mark: while (reader.hasNext()) {
                int next = reader.next();
                switch (next) {
                    case START_ELEMENT:
                        ln = reader.getLocalName();
                        switch (ln) {
                            case RECORD_ELEM:
                                i = 0;
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
                                break;
                            case DATA_ELEM:
                                break mark;
                        }
                        break;
                    case CHARACTERS:
                        if (currAttr == null)
                            throw new IllegalArgumentException("Invalid XML document.");
                        row[i] = reader.getText();
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
            if (attributeName.equals(EsnsiXMLDataFileReadUtil.ATTR_REF))
                return r.getAttributeValue(i);
        }
        return null;
    }

    private static Map<String, ClassifierAttribute> indexAttrs(GetClassifierStructureResponseType struct) {
        return struct.getAttributeList().stream().collect(toMap(ClassifierAttribute::getUid, identity()));
    }

}
