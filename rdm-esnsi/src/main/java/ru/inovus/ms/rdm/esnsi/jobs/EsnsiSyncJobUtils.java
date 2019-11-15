package ru.inovus.ms.rdm.esnsi.jobs;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import ru.inovus.ms.rdm.esnsi.EsnsiSyncException;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.ClassifierDescriptorListType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.xml.stream.XMLStreamConstants.*;
import static ru.inovus.ms.rdm.esnsi.api.AttributeType.*;

final class EsnsiSyncJobUtils {

    static final int PAGE_SIZE = 100;

    private EsnsiSyncJobUtils() {throw new UnsupportedOperationException();}

    static class EsnsiXmlDataFileReadUtil {

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

        private EsnsiXmlDataFileReadUtil() {throw new UnsupportedOperationException();}

        static void read(Consumer<Object[]> consumer, GetClassifierStructureResponseType struct, InputStream inputStream) {
            Map<String, ClassifierAttribute> attributesIdx = struct.getAttributeList().stream().collect(toMap(ClassifierAttribute::getUid, identity()));
            Object[] row = new Object[attributesIdx.size()];
            for (int i = 0; i < row.length; i++)
                row[i] = new StringBuilder();
            try (inputStream) {
                byte[] bytes = inputStream.readAllBytes();
                try (ZipFile zip = new ZipFile(new SeekableInMemoryByteChannel(bytes))) {
                    Iterator<ZipArchiveEntry> iterator = zip.getEntries().asIterator();
                    while (iterator.hasNext()) {
                        ZipArchiveEntry next = iterator.next();
                        try (InputStream in = zip.getInputStream(next)) {
                            readNextEntry(row, in, attributesIdx, consumer);
                        }
                    }
                }
            } catch (IOException e) {
                throw new EsnsiSyncException(e);
            }
        }

        private static void readNextEntry(Object[] row, InputStream inputStream, Map<String, ClassifierAttribute> attributes, Consumer<Object[]> consumer) {
            try {
                XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(inputStream);
                while (reader.hasNext()) {
                    if (reader.next() == START_ELEMENT && reader.getLocalName().equals(DATA_ELEM))
                        break;
                }
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
                                ((StringBuilder) row[currAttr.getOrder()]).append(reader.getText());
                            }
                            break;
                    }
                }
                while (reader.hasNext()) reader.next();
            } catch (XMLStreamException e) {
                throw new EsnsiSyncException(e);
            }
        }

        private static String getAttrValue(XMLStreamReader r) {
            for (int i = 0; i < r.getAttributeCount(); i++) {
                String attributeName = r.getAttributeLocalName(i);
                if (attributeName.equals(EsnsiXmlDataFileReadUtil.ATTR_REF))
                    return r.getAttributeValue(i);
            }
            return null;
        }

    }

    static class XmlDataCreator implements Consumer<String[]> {

        private static final DateTimeFormatter ESNSI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        private static final DateTimeFormatter RDM_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        private static final XMLOutputFactory XML_OUT = XMLOutputFactory.newFactory();

        private final XMLStreamWriter writer;
        private final GetClassifierStructureResponseType struct;
        private final ClassifierAttribute[] attrs;
        private final String[] codes;

        public XmlDataCreator(OutputStream out, GetClassifierStructureResponseType struct) {
            try {
                this.struct = struct;
                this.attrs = struct.getAttributeList().stream().sorted(comparingInt(ClassifierAttribute::getOrder)).toArray(ClassifierAttribute[]::new);
                this.writer = XML_OUT.createXMLStreamWriter(out);
                this.codes = IntStream.rangeClosed(1, attrs.length).mapToObj(i -> "field_" + i).toArray(String[]::new);
            } catch (XMLStreamException e) {
                throw new EsnsiSyncException(e);
            }
        }

        public void init() {
            ClassifierDescriptorListType desc = struct.getClassifierDescriptor();
            try {
                writer.writeStartDocument("1.0");
                writer.writeStartElement("refBook");
                writeLeaf("code", "ESNSI-" + desc.getCode() + "-" + desc.getRevision());
                writer.writeStartElement("passport");
                writeLeaf("name", str(desc.getName(), desc.getCode()));
                writeLeaf("shortName", desc.getCode());
                writeLeaf("description", str(desc.getDescription(), ""));
                writer.writeEndElement();
                writer.writeStartElement("structure");
                for (ClassifierAttribute attr : attrs) {
                    writer.writeStartElement("row");
                    writeLeaf("code", codes[attr.getOrder()]);
                    writeLeaf("name", Objects.toString(attr.getName(), ""));
                    String type = attr.getType() == TEXT ? STRING.value() : attr.getType() == DECIMAL ? "FLOAT" : attr.getType().value();
                    writeLeaf("type", type);
                    writeLeaf("description", "");
                    writeLeaf("primary", String.valueOf(attr.isKey()));
                    if (attr.isRequired())
                        writeValidation("REQUIRED", "true");
                    if (attr.getRegex() != null && !attr.getRegex().isBlank())
                        writeValidation("REG_EXP", attr.getRegex());
                    if (attr.getLength() != null && attr.getLength() > 0)
                        writeValidation("PLAIN_SIZE", attr.getLength().toString());
                    if (attr.getIntStartRange() != null || attr.getIntEndRange() != null)
                        writeValidation("INT_RANGE", (attr.getIntStartRange() == null ? "" : attr.getIntStartRange().toString()) + ";" + (attr.getIntEndRange() == null ? "" : attr.getIntEndRange().toString()));
                    else if (attr.getDecimalStartRange() != null || attr.getDecimalEndRange() != null)
                        writeValidation("FLOAT_RANGE", (attr.getDecimalStartRange() == null ? "" : attr.getDecimalStartRange().toString()) + ";" + (attr.getDecimalEndRange() == null ? "" : attr.getDecimalEndRange().toString()));
                    else if (attr.getDateStartRange() != null || attr.getDateEndRange() != null) {
                        LocalDate start, end;
                        if (attr.getDateStartRange() != null)
                            start = xmlDateToLocalDate(attr.getDateStartRange());
                        else
                            start = LocalDate.MIN;
                        if (attr.getDateEndRange() != null)
                            end = xmlDateToLocalDate(attr.getDateEndRange());
                        else
                            end = LocalDate.MAX;
                        writeValidation("DATE_RANGE", start.format(RDM_DATE_FORMAT) + ";" + end.format(RDM_DATE_FORMAT));
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
                writer.writeStartElement("data");
            } catch (XMLStreamException e) {
                throw new EsnsiSyncException(e);
            }
        }

        private String str(Object obj, String nullDefault) {
            return Objects.toString(obj, nullDefault);
        }

        private LocalDate xmlDateToLocalDate(XMLGregorianCalendar date) {
            return LocalDate.of(
                    date.getYear(),
                    date.getMonth(),
                    date.getDay()
            );
        }

        private void writeValidation(String type, String value) throws XMLStreamException {
            writer.writeStartElement("validation");
            writeLeaf("type", type);
            writeLeaf("value", value);
            writer.writeEndElement();
        }

        @Override
        public void accept(String[] row) {
            try {
                writer.writeStartElement("row");
                for (int i = 0; i < row.length; i++) {
                    String val = row[i];
                    ClassifierAttribute attr = attrs[i];
                    writer.writeStartElement(codes[i]);
                    if (attr.getType() == DATE) {
                        LocalDate date = LocalDate.parse(val, ESNSI_DATE_FORMAT);
                        writer.writeCharacters(date.format(RDM_DATE_FORMAT));
                    } else
                        writer.writeCharacters(val);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new EsnsiSyncException(e);
            }
        }

        public void end() {
            try {
                writer.writeEndElement();
                writer.writeEndElement();
                writer.flush();
                writer.close();
            } catch (XMLStreamException e) {
                throw new EsnsiSyncException(e);
            }
        }

        private void writeLeaf(String key, String val) throws XMLStreamException {
            writer.writeStartElement(key);
            writer.writeCharacters(val);
            writer.writeEndElement();
        }

    }
}
