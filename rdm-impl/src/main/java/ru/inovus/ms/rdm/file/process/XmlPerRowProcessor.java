package ru.inovus.ms.rdm.file.process;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.file.RowMapper;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.*;

import static ru.inovus.ms.rdm.file.process.XmlParseUtils.isStartElementWithName;
import static ru.inovus.ms.rdm.file.process.XmlParseUtils.parseValues;

public class XmlPerRowProcessor extends FilePerRowProcessor {

    private static final Logger logger = LoggerFactory.getLogger(XmlPerRowProcessor.class);

    private static final String XML_READ_ERROR_MESSAGE = "cannot read XML";

    private static final String DATA_TAG_NAME = "data";
    private static final String ROW_TAG_NAME = "row";

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private XMLEventReader reader;


    XmlPerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {
        super(rowMapper, rowsProcessor);
    }

    @Override
    protected void setFile(InputStream inputStream) {

        try {
            FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
            XMLEventReader simpleReader = FACTORY.createXMLEventReader(inputStream);
            reader = FACTORY.createFilteredReader(simpleReader,
                    event ->
                            !(event.isCharacters() && event.asCharacters().isWhiteSpace()));
        } catch (XMLStreamException e) {
            throwXmlReadError(e);
        }
    }

    // check if next tag is <row>. Will move to next tag if meets <data> tag
    @Override
    public boolean hasNext() {
        try {
            if(!reader.hasNext()) {
                return false;
            }

            XMLEvent next = reader.peek();
            if(isStartElementWithName(next, ROW_TAG_NAME)) {
                return true;
            }
            while (!isStartElementWithName(next, DATA_TAG_NAME) && reader.hasNext()) {
                reader.nextEvent();
                next = reader.peek();
            }
            if(!isStartElementWithName(next, DATA_TAG_NAME)) {
                return false;
            }
            reader.nextEvent();
            return isStartElementWithName(reader.peek(), ROW_TAG_NAME);
        } catch (XMLStreamException e) {
            throwXmlReadError(e);
        }
        return false;
    }

    @Override
    public Row next() {
        Map<String, String> rowValues = new LinkedHashMap<>();

        try {
            reader.nextTag();
            if (isStartElementWithName(reader.peek(), DATA_TAG_NAME)) {
                reader.nextTag();
            }
            parseValues(reader, rowValues, ROW_TAG_NAME);

        } catch (XMLStreamException e) {
            logger.error(XML_READ_ERROR_MESSAGE, e);
            throw new NoSuchElementException(XML_READ_ERROR_MESSAGE);
        }

        return new Row(new HashMap<>(rowValues));
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                throwXmlReadError(e);
            }
        }
    }

    private void throwXmlReadError(Exception e) {
        logger.error(XML_READ_ERROR_MESSAGE, e);
        throw new UserException(XML_READ_ERROR_MESSAGE);
    }

}
