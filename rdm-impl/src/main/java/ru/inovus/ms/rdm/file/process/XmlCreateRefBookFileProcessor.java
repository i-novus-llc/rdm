package ru.inovus.ms.rdm.file.process;

import ru.inovus.ms.rdm.n2o.model.refbook.RefBookCreateRequest;
import ru.inovus.ms.rdm.n2o.service.api.RefBookService;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.InputStream;

import static ru.inovus.ms.rdm.file.process.XmlParseUtils.*;

public class XmlCreateRefBookFileProcessor extends CreateRefBookFileProcessor implements Closeable {

    private static final String CODE_TAG_NAME = "code";
    private static final String PASSPORT_TAG_NAME = "passport";
    private static final String STRUCTURE_TAG_NAME = "structure";
    private static final String DATA_TAG_NAME = "data";

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private XMLEventReader reader;


    public XmlCreateRefBookFileProcessor(RefBookService refBookService) {
        super(refBookService);
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

    @Override
    protected RefBookCreateRequest getRefBookCreateRequest() {
        try {
            if(!reader.hasNext()) {
                return null;
            }

            XMLEvent event = reader.nextEvent();
            while (!isStartElementWithName(event, CODE_TAG_NAME, PASSPORT_TAG_NAME, STRUCTURE_TAG_NAME, DATA_TAG_NAME) && reader.hasNext()) {
                event = reader.nextEvent();
            }

            if(isStartElementWithName(event, CODE_TAG_NAME)) {
                return new RefBookCreateRequest(reader.getElementText(), null, null);
            }
        } catch (XMLStreamException e) {
            throwXmlReadError(e);
        }
        return null;
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
}
