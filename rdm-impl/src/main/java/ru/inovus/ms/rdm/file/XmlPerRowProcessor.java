package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.model.Row;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

public class XmlPerRowProcessor extends FilePerRowProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FilePerRowProcessor.class);

    private static final String XML_READ_ERROR_MESSAGE = "cannot read XML";

    private static final String PASSPORT_TAG_NAME = "passport";
    private static final String DATA_TAG_NAME = "data";
    private static final String ROW_TAG_NAME = "row";

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private XMLEventReader reader;

    Map<String, Object> passport;

    private PassportProcessor passportProcessor;


    XmlPerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor, PassportProcessor passportProcessor) {
        super(rowMapper, rowsProcessor);
        this.passportProcessor = passportProcessor;
    }

    @Override
    protected void setFile(InputStream inputStream) {

        try {
            FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
            XMLEventReader simpleReader = FACTORY.createXMLEventReader(inputStream);
            reader = FACTORY.createFilteredReader(simpleReader,
                    event ->
                            !(event.isCharacters() && event.asCharacters().isWhiteSpace()));

            processPassport();

        } catch (XMLStreamException e) {
            throwXmlReadError(e);
        }
    }

    private void processPassport() throws XMLStreamException {
        if (reader.hasNext()) {
            if (reader.peek().isStartDocument())
                reader.nextEvent();

            XMLEvent curEvent = null;
            while (reader.peek() != null && !(isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME))) {
                curEvent = reader.nextEvent();
            }
            if (curEvent == null || reader.peek() == null || isStartElementWithName(reader.peek(), DATA_TAG_NAME))
                return;

            passport = new LinkedHashMap<>();
            reader.nextEvent();     // current is start-tag <passport>
            parseValues(passport, PASSPORT_TAG_NAME);

            if (passport != null)
                passportProcessor.append(passport.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() != null)
                        .collect(toMap(Map.Entry::getKey, e -> (String) e.getValue())));
        }
    }

    private void parseValues(Map<String, Object> map, String outerTagName) throws XMLStreamException {
        String tagName = null;
        String tagValue = null;
        XMLEvent curEvent = reader.nextTag();
        while (curEvent != null && !isEndElementWithName(curEvent, outerTagName)) {
            if (curEvent.isStartElement()) {
                tagName = curEvent.asStartElement().getName().getLocalPart();
            } else if (curEvent.isCharacters()) {
                tagValue = curEvent.asCharacters().getData();
            } else if (curEvent.isEndElement()) {
                map.put(tagName, tagValue);
                tagName = null;
                tagValue = null;
            }
            curEvent = reader.nextEvent();
        }
    }

    private boolean isStartElementWithName(XMLEvent event, String... tagNames) {
        return event != null && event.isStartElement()
                && asList(tagNames).contains(event.asStartElement().getName().getLocalPart());
    }

    private boolean isEndElementWithName(XMLEvent event, String... tagNames) {
        return event.isEndElement()
                && asList(tagNames).contains(event.asEndElement().getName().getLocalPart());
    }

    // check if next tag is <row>. Will move to next tag if meets <data> tag
    @Override
    public boolean hasNext() {
        try {
            XMLEvent next = reader.peek();
            if (isStartElementWithName(next, DATA_TAG_NAME)) {
                reader.nextTag();
                next = reader.peek();
            }
            return isStartElementWithName(next, ROW_TAG_NAME);
        } catch (XMLStreamException e) {
            throwXmlReadError(e);
        }
        return false;
    }

    @Override
    public Row next() {
        Map<String, Object> rowValues = new LinkedHashMap<>();

        try {
            reader.nextTag();
            if (isStartElementWithName(reader.peek(), DATA_TAG_NAME)) {
                reader.nextTag();
            }
            parseValues(rowValues, ROW_TAG_NAME);

        } catch (XMLStreamException e) {
            throwXmlReadError(e);
        }

        return new Row(rowValues);
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
