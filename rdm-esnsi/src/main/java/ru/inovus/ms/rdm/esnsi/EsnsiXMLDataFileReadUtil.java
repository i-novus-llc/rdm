package ru.inovus.ms.rdm.esnsi;

import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.xml.stream.XMLStreamConstants.*;

class EsnsiXMLDataFileReadUtil {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newFactory();
    static {
        INPUT_FACTORY.setProperty("javax.xml.stream.isCoalescing", true);
    }

    private static final String DATA_ELEM = "data";
    private static final String RECORD_ELEM = "record";
    private static final String ATTR_VALUE = "attribute-value";
    private static final String ATTR_REF = "attribute-ref";

    private static final Set<String> ATTR_TYPES = Set.of(
        "string",
        "text",
        "bool",
        "date",
        "integer",
        "decimal",
        "reference"
    );

    private EsnsiXMLDataFileReadUtil() {throw new UnsupportedOperationException();}

    static void read(Consumer<Object[]> consumer, GetClassifierStructureResponseType struct, InputStream inputStream) {
        Map<String, ClassifierAttribute> attributes = indexAttrs(struct);
        Object[] row = new Object[attributes.size()];
        for (int i = 0; i < row.length; i++)
            row[i] = new StringBuilder();
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            while (zis.getNextEntry() != null) {
                readNextEntry(row, new NonClosingInputStream(zis), attributes, consumer);
            }
        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    private static void readNextEntry(Object[] row, InputStream inputStream, Map<String, ClassifierAttribute> attributes, Consumer<Object[]> consumer) {
        try {
            XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(inputStream);
            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT && reader.getLocalName().equals(DATA_ELEM))
                    break;
            }
            int i = 0;
            ClassifierAttribute currAttr = null;
            boolean recordOpen = false;
            String openedLocalName = null;
            mark: while (reader.hasNext()) {
                int next = reader.next();
                switch (next) {
                    case START_ELEMENT:
                        openedLocalName = reader.getLocalName();
                        switch (openedLocalName) {
                            case RECORD_ELEM:
                                i = 0;
                                recordOpen = true;
                                break;
                            case ATTR_VALUE:
                                currAttr = attributes.get(getAttrValue(reader));
                                break;
                        }
                        break;
                    case END_ELEMENT:
                        openedLocalName = reader.getLocalName();
                        switch (openedLocalName) {
                            case RECORD_ELEM:
                                recordOpen = false;
                                consumer.accept(stream(row).map(obj -> (StringBuilder) obj).map(StringBuilder::toString).toArray());
                                stream(row).map(obj -> (StringBuilder) obj).forEach(stringBuilder -> stringBuilder.setLength(0));
                                break;
                            case DATA_ELEM:
                                break mark;
                        }
                        break;
                    case CHARACTERS:
                        if (!reader.isWhiteSpace() && recordOpen && ATTR_TYPES.contains(openedLocalName)) {
                            if (currAttr == null)
                                throw new IllegalArgumentException("Invalid XML document.");
                            ((StringBuilder) row[i++]).append(reader.getText());
                        }
                        break;
                }
            }
            while (reader.hasNext()) reader.next();
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

    private static class NonClosingInputStream extends InputStream {

        private final InputStream inputStream;

        NonClosingInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

    }

}
