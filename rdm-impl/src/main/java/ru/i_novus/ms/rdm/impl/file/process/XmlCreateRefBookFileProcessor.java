package ru.i_novus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.exception.FileContentException;
import ru.i_novus.ms.rdm.api.exception.FileProcessingException;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.service.RefBookService;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.InputStream;

import static ru.i_novus.ms.rdm.impl.file.process.XmlParseUtils.*;

public class XmlCreateRefBookFileProcessor extends CreateRefBookFileProcessor implements Closeable {

    private static final String REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE = "refbook.is.not.created";

    private static final String CODE_TAG_NAME = "code";
    private static final String TYPE_TAG_NAME = "type";
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
        reader = createEventReader(inputStream, FACTORY);
    }

    @Override
    protected RefBookCreateRequest getRefBookCreateRequest() {

        String refBookCode = null;
        String refBookType = null;
        try {
            if(!reader.hasNext()) {
                return null;
            }

            XMLEvent event = getNextTagEvent();
            if (isStartElementWithName(event, CODE_TAG_NAME)) {
                refBookCode = reader.getElementText();
            }

            event = getNextTagEvent();
            if (isStartElementWithName(event, TYPE_TAG_NAME)) {
                refBookType = reader.getElementText();
            }

        } catch (XMLStreamException e) {
            throw new FileContentException(e);

        } catch (Exception e) {
            if (e.getCause() instanceof XMLStreamException)
                throw new FileContentException(e);

            throw new FileProcessingException(e);
        }

        if (!StringUtils.isEmpty(refBookCode)) {
            return new RefBookCreateRequest(refBookCode, refBookType, null, null);
        }

        throw new UserException(REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE);
    }

    private XMLEvent getNextTagEvent() throws XMLStreamException {

        XMLEvent event = reader.nextEvent();
        while (!isStartElementWithName(event,
                CODE_TAG_NAME, TYPE_TAG_NAME, PASSPORT_TAG_NAME, STRUCTURE_TAG_NAME, DATA_TAG_NAME)
                && reader.hasNext()) {
            event = reader.nextEvent();
        }
        return event;
    }

    @Override
    public void close() {
        closeEventReader(reader);
    }
}
