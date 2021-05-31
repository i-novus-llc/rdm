package ru.i_novus.ms.rdm.impl.file.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class XmlFileGenerator extends PerRowFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(XmlFileGenerator.class);

    private static final String CODE_TAG_NAME = "code";
    private static final String TYPE_TAG_NAME = "type";
    private static final String PASSPORT_TAG_NAME = "passport";
    private static final String STRUCTURE_TAG_NAME = "structure";
    private static final String DATA_TAG_NAME = "data";
    private static final String ROW_TAG_NAME = "row";

    private static final String XML_GENERATE_ERROR_MESSAGE = "cannot generate XML";

    private final Map<String, Structure.Reference> attributeToReferenceMap;

    private final Map<String, String> passport;

    private final RefBookVersion version;

    private final List<AttributeValidation> attributeValidations;

    private XMLStreamWriter writer;

    /**
     *
     * @param rowIterator             Итератор по записям версии справочника
     * @param version                 Версия справочника
     * @param attributeToReferenceMap Набор атрибутов-ссылок в формате:
     *                                key - код атрибута-ссылки,
     *                                value - атрибут-ссылка
     * @param attributeValidations    Список валидаций атрибутов
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

            writer.writeStartElement(DATA_TAG_NAME);

        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    @Override
    protected void write(Row row) {
        try {
            writer.writeStartElement(ROW_TAG_NAME);
            for (String fieldCode : getStructure().getAttributeCodes()) {
                if (row.getData().get(fieldCode) != null) {
                    Object value = row.getData().get(fieldCode);
                    String stringValue = ConverterUtil.toStringValue((Serializable) value);
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
            writer.writeEndElement(); // DATA_TAG_NAME
            writer.writeEndElement(); // refBook
            writer.writeEndDocument();
            writer.flush();

            logger.info("XML generate finished");

        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    private void addPassport() throws XMLStreamException {

        writer.writeStartElement(PASSPORT_TAG_NAME);
        passport.keySet().forEach(v -> {
            try {
                writer.writeStartElement(v);
                writer.writeCharacters(passport.get(v));
                writer.writeEndElement();

            } catch (XMLStreamException e) {
                throwXmlGenerateError(e);
            }
        });
        writer.writeEndElement();
    }

    private void addFields() {

        writeElement(CODE_TAG_NAME, version.getCode());

        if (!RefBookTypeEnum.DEFAULT.equals(version.getType())) {
            writeElement(TYPE_TAG_NAME, version.getType().name());
        }
    }

    private void addStructure() {
        try {
            writer.writeStartElement(STRUCTURE_TAG_NAME);
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

        if (!attribute.isReferenceType())
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

        if (attributeValidations == null)
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

        if (body == null)
            return;

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
