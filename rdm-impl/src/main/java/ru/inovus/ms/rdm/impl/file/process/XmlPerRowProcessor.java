package ru.inovus.ms.rdm.impl.file.process;

import ru.inovus.ms.rdm.api.exception.FileContentException;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.impl.util.mappers.RowMapper;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static ru.inovus.ms.rdm.impl.file.process.XmlParseUtils.*;

public class XmlPerRowProcessor extends FilePerRowProcessor {

    public static final String FILE_CONTENT_INVALID_EXCEPTION_CODE = "file.content.invalid";

    private static final String DATA_TAG_NAME = "data";
    private static final String ROW_TAG_NAME = "row";

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private XMLEventReader reader;

    XmlPerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {
        super(rowMapper, rowsProcessor);
    }

    @Override
    protected void setFile(InputStream inputStream) {
        reader = createEventReader(inputStream, FACTORY);
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
            throw new FileContentException(e);
        }
    }

    @Override
    public Row next() {
        Map<String, Object> rowValues = new LinkedHashMap<>();
        try {
            reader.nextTag();
            if (isStartElementWithName(reader.peek(), DATA_TAG_NAME)) {
                reader.nextTag();
            }
            parseValues(reader, rowValues, ROW_TAG_NAME);

        } catch (XMLStreamException e) {
            // by contract of this method:
            throw new NoSuchElementException(FILE_CONTENT_INVALID_EXCEPTION_CODE);
        }

        return new Row(new HashMap<>(rowValues));
    }

    @Override
    public void close() {
        closeEventReader(reader);
    }
}
