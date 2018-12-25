package ru.inovus.ms.rdm.file.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.util.TimeUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map;

public class XmlFileGenerator extends PerRowFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(XmlFileGenerator.class);

    private static final String XML_GENERATE_ERROR_MESSAGE = "cannot generate XML";

    private XMLStreamWriter writer;

    private Map<String, String> passport;

    public XmlFileGenerator(Iterator<Row> rowIterator, RefBookVersion version) {
        super(rowIterator, version.getStructure());
        this.passport = version.getPassport();
    }

    @Override
    protected void startWrite() {
        logger.info("Start generate XML");

        try {
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(getOutputStream());
            writer.writeStartDocument("1.0");
            writer.writeStartElement("refBook");

            addPassport();

            writer.writeStartElement("data");
        } catch (XMLStreamException e) {
            throwXmlGenerateError(e);
        }
    }

    @Override
    protected void write(Row row) {
        try {
            writer.writeStartElement("row");
            for (String fieldCode : row.getData().keySet()) {
                if (getStructure().getAttribute(fieldCode) != null
                        && row.getData().get(fieldCode) != null) {
                    String stringValue = getStringValue(row.getData().get(fieldCode));
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

    private String getStringValue(Object value) {
        if (value instanceof LocalDate) {
            return TimeUtils.format((LocalDate) value);
        }
        if (value instanceof Reference) {
            return ((Reference) value).getValue();
        }
        return String.valueOf(value);
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
