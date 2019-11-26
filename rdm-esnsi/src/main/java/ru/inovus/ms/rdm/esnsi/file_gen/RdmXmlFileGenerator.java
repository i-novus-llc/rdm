package ru.inovus.ms.rdm.esnsi.file_gen;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class RdmXmlFileGenerator implements Consumer<Map<String, Object>> {

    public static final DateTimeFormatter RDM_DATE_FORMAT = TimeUtils.DATE_PATTERN_EUROPEAN_FORMATTER;

    private static final XMLOutputFactory XML_OUT = XMLOutputFactory.newFactory();

    private final XMLStreamWriter writer;
    private final RefBookMetadata metadata;
    private final RefBookStructure structure;
    private final Map<String, ? extends Collection<AttributeValidation>> validationsMap;
    private final Map<String, RefBookStructure.Attribute> indexAttrs;
    private final Map<String, RefBookStructure.Reference> indexRefs;
    private final BiFunction<String, Object, String> dateToRdmStr;
    private final BiFunction<String, Object, String> refToRdmStr;
    private final Iterator<Map<String, Object>> cursor;
    private final String[] attrsOrder;
    private final int numAttrs;

    public RdmXmlFileGenerator(OutputStream out, RefBookMetadata metadata, RefBookStructure structure, Map<String, ? extends Collection<AttributeValidation>> validationsMap, BiFunction<String, Object, String> dateToRdmStr, BiFunction<String, Object, String> refToRdmStr) throws XMLStreamException {
        this(out, metadata, structure, validationsMap, dateToRdmStr, refToRdmStr, null);
    }

    public RdmXmlFileGenerator(OutputStream out, RefBookMetadata metadata, RefBookStructure structure, Map<String, ? extends Collection<AttributeValidation>> validationsMap, BiFunction<String, Object, String> dateToRdmStr, BiFunction<String, Object, String> refToRdmStr, Iterator<Map<String, Object>> cursor) throws XMLStreamException {
        this.writer = XML_OUT.createXMLStreamWriter(out);
        this.metadata = metadata;
        this.structure = structure;
        this.validationsMap = validationsMap;
        this.indexAttrs = structure.attributes().stream().collect(toMap(RefBookStructure.Attribute::code, identity()));
        this.indexRefs = structure.references().stream().collect(toMap(RefBookStructure.Reference::attribute, identity()));
        this.dateToRdmStr = dateToRdmStr;
        this.refToRdmStr = refToRdmStr;
        this.cursor = cursor;
        this.attrsOrder = structure.attributes().stream().map(RefBookStructure.Attribute::code).toArray(String[]::new);
        this.numAttrs = structure.attributes().size();
    }

    public void init() {
        try {
            writer.writeStartDocument("1.0");
            writer.writeStartElement("refBook");
            writeLeaf("code", metadata.code());
            writer.writeStartElement("passport");
            writeLeaf("name", metadata.name());
            writeLeaf("shortName", metadata.shortName());
            writeLeaf("description", metadata.description());
            writer.writeEndElement();
            writer.writeStartElement("structure");
            for (RefBookStructure.Attribute attr : structure.attributes()) {
                writeNextAttr(attr);
            }
            writer.writeEndElement();
            writer.writeStartElement("data");
        } catch (XMLStreamException e) {
            throw new RdmException(e);
        }
    }

    private void writeNextAttr(RefBookStructure.Attribute attr) throws XMLStreamException {
        writer.writeStartElement("row");
        writeLeaf("code", attr.code());
        writeLeaf("name", attr.name());
        writeLeaf("type", attr.type().name());
        if (attr.description() != null && !attr.description().isBlank())
            writeLeaf("description", attr.description());
        writeLeaf("primary", Boolean.toString(attr.isPrimary()));
        if (attr.type() == FieldType.REFERENCE) {
            RefBookStructure.Reference reference = indexRefs.get(attr.code());
            writeLeaf("referenceCode", reference.referenceCode());
            if (reference.displayExpression() != null)
                writeLeaf("displayExpression", reference.displayExpression());
        }
        Collection<AttributeValidation> validations = this.validationsMap.get(attr.code());
        if (validations != null && !validations.isEmpty()) {
            for (AttributeValidation validation : validations) {
                writeValidation(validation.type(), validation.value());
            }
        }
        writer.writeEndElement();
    }

    private void writeValidation(AttributeValidationType type, String value) throws XMLStreamException {
        writer.writeStartElement("validation");
        writeLeaf("type", type.name());
        writeLeaf("value", value);
        writer.writeEndElement();
    }

    public void fetchData() {
        if (cursor == null)
            throw new IllegalStateException("Provide iterator first.");
        while (cursor.hasNext()) {
            Map<String, Object> row = cursor.next();
            accept(row);
        }
    }

    @Override
    public void accept(Map<String, Object> row) {
        try {
            writer.writeStartElement("row");
            for (int i = 0; i < numAttrs; i++) {
                String key = attrsOrder[i];
                RefBookStructure.Attribute attribute = indexAttrs.get(key);
                writer.writeStartElement(attribute.code());
                Object val = row.get(key);
                String valStr;
                if (attribute.type() == FieldType.DATE) {
                    valStr = dateToRdmStr.apply(attribute.code(), val);
                } else if (attribute.type() == FieldType.REFERENCE) {
                    valStr = refToRdmStr.apply(attribute.code(), val);
                } else {
                    valStr = val.toString();
                }
                writer.writeCharacters(valStr);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RdmException(e);
        }
    }

    private void writeLeaf(String key, String val) throws XMLStreamException {
        writer.writeStartElement(key);
        writer.writeCharacters(val);
        writer.writeEndElement();
    }

    public void end() {
        try {
            writer.writeEndElement();
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new RdmException(e);
        }
    }

}
