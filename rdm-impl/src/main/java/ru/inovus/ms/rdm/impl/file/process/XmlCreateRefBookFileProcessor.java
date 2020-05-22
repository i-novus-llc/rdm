package ru.inovus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.inovus.ms.rdm.api.service.RefBookService;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.InputStream;

import static ru.inovus.ms.rdm.impl.file.process.FileParseUtils.*;
import static ru.inovus.ms.rdm.impl.file.process.XmlParseUtils.*;

public class XmlCreateRefBookFileProcessor extends CreateRefBookFileProcessor implements Closeable {

    private static final String REFBOOK_DOES_NOT_CREATE_EXCEPTION_CODE = "refbook.does.not.create";

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
        reader = createEvenReader(inputStream, FACTORY);
    }

    @Override
    protected RefBookCreateRequest getRefBookCreateRequest() {

        String refBookCode = null;
        try {
            if(!reader.hasNext()) {
                return null;
            }

            XMLEvent event = reader.nextEvent();
            while (!isStartElementWithName(event, CODE_TAG_NAME, PASSPORT_TAG_NAME, STRUCTURE_TAG_NAME, DATA_TAG_NAME) && reader.hasNext()) {
                event = reader.nextEvent();
            }

            if(isStartElementWithName(event, CODE_TAG_NAME)) {
                refBookCode = reader.getElementText();
            }

        } catch (XMLStreamException e) {
            throwFileContentError(e);

        } catch (Exception e) {
            if (e.getCause() instanceof XMLStreamException)
                throwFileContentError(e);

            throwFileProcessingError(e);
        }

        if (!StringUtils.isEmpty(refBookCode)) {
            return new RefBookCreateRequest(refBookCode, null, null);
        }

        throw new UserException(FILE_PROCESSING_FAILED_EXCEPTION_CODE, new UserException(REFBOOK_DOES_NOT_CREATE_EXCEPTION_CODE));
    }

    @Override
    public void close() {
        closeEventReader(reader);
    }
}
