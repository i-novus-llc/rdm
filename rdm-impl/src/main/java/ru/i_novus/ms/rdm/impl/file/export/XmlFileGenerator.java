package ru.i_novus.ms.rdm.impl.file.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class XmlFileGenerator extends PerRowFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(XmlFileGenerator.class);

    private static final String XML_GENERATE_ERROR_MESSAGE = "cannot generate XML";

    private Map<String, Structure.Reference> attributeToReferenceMap;

    private XMLStreamWriter writer;

    private Map<String, String> passport;

    private RefBookVersion version;

    private List<AttributeValidation> attributeValidations;

    /**
     *
     * @param rowIterator
     * @param version
     * @param attributeToReferenceMap - key - код  ссылочного атрибута ссылки, value - код справочника на который ссылаются
     * @param attributeValidations
     */
    public XmlFileGenerator(Iterator<Row> rowIterator,
                            RefBookVersion version,
                            Map<String, Structure.Reference> attributeToReferenceMap,
                            List<AttributeValidation> attributeValidations) {
        super(rowIterator, version.getStructure());
        this.passport = version.getPassport();
        this.attributeToReferenceMap = attributeToReferenceMap;
        this.attributeValidations = attributeValidations;
        this.version = version;
    }

    @Override
    protected void startWrite() {
        logger.info("Start generate XML");

        try {
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(getOutputStream());
            writer.writeStartDocument("1.0");
            writer.writeStartElement("refBook");

            addFields();
            addPassport();
            addStructure();

            writer.writeStartElement("data");
        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    @Override
    protected void write(Row row) {
        try {
            writer.writeStartElement("row");
            for (String fieldCode : getStructure().getAttributeCodes()) {
                if (row.getData().get(fieldCode) != null) {
                    String stringValue = ConverterUtil.toString(row.getData().get(fieldCode));
                    if (stringValue == null) {
                        writer.writeEmptyElement(fieldCode);
                    } else {
                        writer.writeStartElement(fieldCode);
                        writer.writeCharacters(stringValue);
                        writer.writeEndElement();
                    }
                }
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    @Override
    protected void endWrite() {
        try {
            writer.writeEndElement(); //data
            writer.writeEndElement(); //refBook
            writer.writeEndDocument();
            writer.flush();
            logger.info("XML generate finished");
        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    private void addPassport() throws XMLStreamException {
        writer.writeStartElement("passport");
        passport.keySet().forEach(v -> {
            try {
                writer.writeStartElement(v);
                writer.writeCharacters(passport.get(v));
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throwXmlGenerateError(e);
            }
        });
        writer.writeEndElement(); //passport
    }

    private void addFields() {

        writeElement("code", version.getCode());
        writeElement("type",  version.getType() == null ? null : version.getType().name());
    }

    private void addStructure() {
        try {
            writer.writeStartElement("structure");
            version.getStructure().getAttributes().forEach(attribute -> {
                try {
                    writer.writeStartElement("row");
                    addAttribute(attribute);
                    addAttributeValidation(attribute.getCode());
                    writer.writeEndElement();

                } catch (XMLStreamException e) {
                    throwXmlGenerateError(e);
                }
            });
            writer.writeEndElement();

        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    private void addAttribute(Structure.Attribute attribute) {

        writeElement("code", attribute.getCode());
        writeElement("name", attribute.getName());
        writeElement("type", attribute.getType().name());
        writeElement("primary", "" + attribute.hasIsPrimary());

        if (attribute.isLocalizable()) {
            writeElement("localizable", "true");
        }
        writeElement("description", attribute.getDescription());

        addReference(attribute);
    }

    private void addReference(Structure.Attribute attribute) {

        if(!attribute.isReferenceType())
            return;

        Structure.Reference reference = attributeToReferenceMap.get(attribute.getCode());
        if (reference == null)
            throw new RdmException("reference.not.found");

        writeElement("referenceCode", reference.getReferenceCode());
        if (reference.getDisplayExpression() != null) {
            writeElement("displayExpression", reference.getDisplayExpression());
        }
    }

    private void addAttributeValidation(String attributeCode) {

        if(attributeValidations == null)
            return;

        attributeValidations.stream()
                .filter(validation -> validation.getAttribute().equals(attributeCode))
                .forEach(validation -> {
                    try {
                        writer.writeStartElement("validation");
                        writeElement("type", validation.getType().name());

                        final String value = validation.valuesToString();
                        if (value != null) {
                            writeElement("value", value);
                        }
                        writer.writeEndElement();

                    } catch (XMLStreamException e) {
                        throwXmlGenerateError(e);
                    }
                });

    }

    private void writeElement(String elementName, String body) {
        if(body == null) {
            return;
        }
        try {
            writer.writeStartElement(elementName);
            writer.writeCharacters(body);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    private void throwXmlGenerateError(XMLStreamException e) {

        logger.error(XML_GENERATE_ERROR_MESSAGE, e);
        throw new RdmException(XML_GENERATE_ERROR_MESSAGE);
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (XMLStreamException e) {
            logger.error("cannot close output", e);
        }
    }
}
