package ru.inovus.ms.rdm.esnsi.sync;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;
import ru.inovus.ms.rdm.esnsi.file_gen.AttributeValidation;
import ru.inovus.ms.rdm.esnsi.file_gen.RdmXmlFileGenerator;
import ru.inovus.ms.rdm.esnsi.file_gen.RefBookMetadata;
import ru.inovus.ms.rdm.esnsi.file_gen.RefBookStructure;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.xml.stream.XMLStreamConstants.*;
import static ru.inovus.ms.rdm.esnsi.EsnsiLoaderDao.FIELD_PREFIX;
import static ru.inovus.ms.rdm.esnsi.api.AttributeType.DECIMAL;
import static ru.inovus.ms.rdm.esnsi.api.AttributeType.TEXT;
import static ru.inovus.ms.rdm.esnsi.file_gen.RdmXmlFileGenerator.RDM_DATE_FORMAT;

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
                "decimal"
        );

        private EsnsiXmlDataFileReadUtil() {throw new UnsupportedOperationException();}

        static void read(Consumer<Map<String, String>> consumer, GetClassifierStructureResponseType struct, InputStream inputStream) {
            Map<String, ClassifierAttribute> attributesIdx = struct.getAttributeList().stream().collect(toMap(ClassifierAttribute::getUid, identity()));
            Object[] row = new Object[attributesIdx.size()];
            for (int i = 0; i < row.length; i++)
                row[i] = new StringBuilder();
            try (inputStream) {
                byte[] bytes = IOUtils.toByteArray(inputStream);
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
                throw new RdmException(e);
            }
        }

        @SuppressWarnings("squid:S3776")
        private static void readNextEntry(Object[] row, InputStream inputStream, Map<String, ClassifierAttribute> attributes, Consumer<Map<String, String>> consumer) {
            try {
                XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(inputStream);
                flushUntilDataElem(reader);
                ClassifierAttribute currAttr = null;
                boolean recordOpen = false;
                String openedLocalName = null;
                while (reader.hasNext()) {
                    int next = reader.next();
                    switch (next) {
                        case START_ELEMENT:
                            openedLocalName = reader.getLocalName();
                            if (openedLocalName.equals(RECORD_ELEM))
                                recordOpen = true;
                            else if (openedLocalName.equals(ATTR_VALUE))
                                currAttr = attributes.get(getAttrValue(reader));
                            break;
                        case END_ELEMENT:
                            openedLocalName = reader.getLocalName();
                            if (openedLocalName.equals(RECORD_ELEM)) {
                                recordOpen = false;
                                Map<String, String> map = IntStream.rangeClosed(1, row.length).boxed().collect(toMap(i -> FIELD_PREFIX + i, i -> row[i - 1].toString()));
                                consumer.accept(map);
                                stream(row).map(obj -> (StringBuilder) obj).forEach(stringBuilder -> stringBuilder.setLength(0));
                            } else if (openedLocalName.equals(DATA_ELEM))
                                flushCompletely(reader);
                            break;
                        case CHARACTERS:
                            if (!reader.isWhiteSpace() && recordOpen && ATTR_TYPES.contains(openedLocalName))
                                setNextVal(currAttr, row, reader);
                            break;
                        default:
                            break;
                    }
                }
            } catch (XMLStreamException e) {
                throw new RdmException(e);
            }
        }

        private static void flushUntilDataElem(XMLStreamReader reader) throws XMLStreamException {
            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT && reader.getLocalName().equals(DATA_ELEM))
                    break;
            }
        }

        private static void flushCompletely(XMLStreamReader reader) throws XMLStreamException {
            while (reader.hasNext()) reader.next();
        }

        private static void setNextVal(ClassifierAttribute currAttr, Object[] row, XMLStreamReader reader) {
            if (currAttr == null)
                throw new IllegalArgumentException("Invalid XML document.");
            ((StringBuilder) row[currAttr.getOrder()]).append(reader.getText());
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

    static final class RdmXmlFileGeneratorProvider {

        private RdmXmlFileGeneratorProvider() {throw new UnsupportedOperationException();}

        @SuppressWarnings("squid:S3776")
        static RdmXmlFileGenerator get(OutputStream out, GetClassifierStructureResponseType struct, Iterator<Map<String, Object>> iterator, String dateFormatsStr) throws XMLStreamException {
            String[] split = dateFormatsStr.split(",");
            DateTimeFormatter[] formatters = new DateTimeFormatter[split.length];
            for (int i = 0; i < formatters.length; i++)
                formatters[i] = DateTimeFormatter.ofPattern(split[i].trim());
            RefBookMetadata refBookMetadataAdapter = new RefBookMetadata() {
                @Override public String code() {return "ESNSI-" + struct.getClassifierDescriptor().getPublicId();}
                @Override public String name() {return struct.getClassifierDescriptor().getName();}
                @Override public String shortName() {return struct.getClassifierDescriptor().getName();}
                @Override public String description() {return struct.getClassifierDescriptor().getDescription();}
            };
            RefBookStructure refBookStructureAdapter = new RefBookStructure() {
                @Override
                public Collection<Attribute> attributes() {
                    Collection<Attribute> attributes = new ArrayList<>();
                    for (ClassifierAttribute attribute : struct.getAttributeList()) {
                        attributes.add(new Attribute() {
                            @Override
                            public String code() {
                                return FIELD_PREFIX + (attribute.getOrder() + 1);
                            }

                            @Override
                            public String name() {
                                return attribute.getName();
                            }

                            @Override
                            public String description() {
                                return attribute.getName();
                            }

                            @Override
                            public FieldType type() {
                                if (attribute.getType() == TEXT)
                                    return FieldType.STRING;
                                else if (attribute.getType() == DECIMAL)
                                    return FieldType.FLOAT;
                                else
                                    return FieldType.valueOf(attribute.getType().value());
                            }

                            @Override
                            public boolean isPrimary() {
                                return attribute.isKey();
                            }
                        });
                    }

                    return attributes;
                }

                @Override
                public Collection<Reference> references() {
                    return emptyList(); // Пока не реализовано
                }
            };
            Map<String, Collection<AttributeValidation>> validations = new HashMap<>();
            for (ClassifierAttribute attribute : struct.getAttributeList()) {
                if (attribute.isRequired())
                    validations.computeIfAbsent(FIELD_PREFIX + attribute.getOrder() + 1, k -> new ArrayList<>()).add(new AttributeValidation(AttributeValidationType.REQUIRED, "true"));
                if (attribute.getRegex() != null && !attribute.getRegex().isBlank())
                    validations.computeIfAbsent(FIELD_PREFIX + attribute.getOrder() + 1, k -> new ArrayList<>()).add(new AttributeValidation(AttributeValidationType.REG_EXP, attribute.getRegex()));
                if (attribute.getLength() != null && attribute.getLength() > 0)
                    validations.computeIfAbsent(FIELD_PREFIX + attribute.getOrder() + 1, k -> new ArrayList<>()).add(new AttributeValidation(AttributeValidationType.PLAIN_SIZE, attribute.getLength().toString()));
                if (attribute.getIntStartRange() != null || attribute.getIntEndRange() != null)
                    validations.computeIfAbsent(FIELD_PREFIX + attribute.getOrder() + 1, k -> new ArrayList<>()).add(new AttributeValidation(AttributeValidationType.INT_RANGE, getNumberRangeValidation(attribute.getIntStartRange(), attribute.getIntEndRange())));
                if (attribute.getDecimalStartRange() != null || attribute.getDecimalEndRange() != null)
                    validations.computeIfAbsent(FIELD_PREFIX + attribute.getOrder() + 1, k -> new ArrayList<>()).add(new AttributeValidation(AttributeValidationType.FLOAT_RANGE, getNumberRangeValidation(attribute.getDecimalStartRange(), attribute.getDecimalEndRange())));
                if (attribute.getDateStartRange() != null || attribute.getDateEndRange() != null)
                    validations.computeIfAbsent(FIELD_PREFIX + attribute.getOrder() + 1, k -> new ArrayList<>()).add(new AttributeValidation(AttributeValidationType.DATE_RANGE, getDateRangeValidation(attribute.getDateStartRange(), attribute.getDateEndRange())));
            }
            return new RdmXmlFileGenerator(out, refBookMetadataAdapter, refBookStructureAdapter, validations, new BiFunction<>() {
                Map<String, DateTimeFormatter> fieldToDateFormat = new HashMap<>();

                @Override
                public String apply(String s, Object o) {
                    LocalDate date = parseDate(s, (String) o);
                    if (date == null)
                        return "";
                    return date.format(RDM_DATE_FORMAT);
                }

                private LocalDate parseDate(String fieldCode, String date) {
                    if(date == null || date.trim().isEmpty()) {
                        return null;
                    }
                    if (fieldToDateFormat.containsKey(fieldCode))
                        return parseDate(date, fieldToDateFormat.get(fieldCode));
                    for (DateTimeFormatter formatter : formatters) {
                        LocalDate localDate = parseDate(date, formatter);
                        if (localDate != null) {
                            fieldToDateFormat.put(fieldCode, formatter);
                            return localDate;
                        }
                    }
                    throw new RdmException("Unable to parse date from ESNSI.");
                }

                private LocalDate parseDate(String date, DateTimeFormatter format) {
                    try {
                        return LocalDate.parse(date, format);
                    } catch (DateTimeParseException ignored) {
                        return null;
                    }
                }

            }, null, iterator);
        }

        private static String getNumberRangeValidation(Number from, Number to) {
            String fromStr = str(from, "");
            String toStr = str(to, "");
            return fromStr + ";" + toStr;
        }

        private static String getDateRangeValidation(XMLGregorianCalendar from, XMLGregorianCalendar to) {
            String f = "";
            String t = "";
            if (from != null)
                f = LocalDate.of(from.getYear(), from.getMonth(), from.getDay()).format(RDM_DATE_FORMAT);
            if (to != null)
                t = LocalDate.of(to.getYear(), to.getMonth(), to.getDay()).format(RDM_DATE_FORMAT);
            return f + ";" + t;
        }

        private static String str(Object obj, String nullDefault) {
            return Objects.toString(obj, nullDefault);
        }

    }

}
