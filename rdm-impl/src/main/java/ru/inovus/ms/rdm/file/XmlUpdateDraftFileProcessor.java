package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.DraftService;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.inovus.ms.rdm.file.XmlParseUtils.*;
import static ru.inovus.ms.rdm.file.XmlParseUtils.isStartElementWithName;

public class XmlUpdateDraftFileProcessor extends UpdateDraftFileProcessor implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(FilePerRowProcessor.class);

    private static final String XML_READ_ERROR_MESSAGE = "cannot read XML";

    private static final String PASSPORT_TAG_NAME = "passport";
    private static final String STRUCTURE_TAG_NAME = "structure";
    private static final String DATA_TAG_NAME = "data";
    private static final String ROW_TAG_NAME = "row";

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private XMLEventReader reader;

    private Map<String, String> passport;

    private boolean passportProcessed = false;

    public XmlUpdateDraftFileProcessor(Integer refBookId, DraftService draftService) {
        super(refBookId, draftService);
    }

    @Override
    public Map<String, String> getPassport(){
        if(passportProcessed) {
            return passport;
        }
        try {
            if (reader.hasNext()) {
                if (reader.peek().isStartDocument())
                    reader.nextEvent();

                XMLEvent curEvent = null;
                while (reader.peek() != null && !(isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, STRUCTURE_TAG_NAME, DATA_TAG_NAME))) {
                    curEvent = reader.nextEvent();
                }
                if (curEvent == null || reader.peek() == null || isStartElementWithName(reader.peek(), STRUCTURE_TAG_NAME) || isStartElementWithName(reader.peek(), DATA_TAG_NAME))
                    return null;

                passport = new LinkedHashMap<>();
                reader.nextEvent();     // current is start-tag <passport>
                parseValues(reader, passport, PASSPORT_TAG_NAME);
            }
        } catch (XMLStreamException e) {
            throwXmlReadError(e);
        }
        passportProcessed = true;
        return passport;

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
    protected Structure getStructure() {
        if (reader.hasNext()) {
            try {
                XMLEvent curEvent = reader.peek();
                while (curEvent != null && !(isStartElementWithName(curEvent, STRUCTURE_TAG_NAME, DATA_TAG_NAME))) {
                    reader.nextEvent();
                    curEvent = reader.peek();
                }
                if (curEvent == null || reader.peek() == null || isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME))
                    return null;

                Structure structure = new Structure(new ArrayList(), new ArrayList<>());
                reader.nextEvent();
                while (!isEndElementWithName(reader.peek(), STRUCTURE_TAG_NAME) && !isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME)) {
                    Map<String, String> attribute = new LinkedHashMap<>();
                    XMLEvent xmlEvent = reader.nextEvent();// current is <row> in <structure>
                    parseValues(reader, attribute, ROW_TAG_NAME);
                    Structure.Attribute structureAttribute = new Structure.Attribute();
                    structureAttribute.setCode((String) attribute.get("code"));
                    structureAttribute.setDescription((String) attribute.get("description"));
                    structureAttribute.setName((String) attribute.get("name"));
                    structureAttribute.setType(FieldType.valueOf((String) attribute.get("type")));
                    structureAttribute.setPrimary(Boolean.valueOf((String)attribute.get("primary")));
                    structure.getAttributes().add(structureAttribute);
                    if(FieldType.REFERENCE.equals(structureAttribute.getType())) {
                        String referenceCode = (String) attribute.get("referenceCode");
                        if(referenceCode != null) {
                            // need implement
                        }
                    }
                }
                reader.nextTag();

                structure.setReferences(new ArrayList<>()); // NB: Убрать, когда переделаем ссылочность

                return structure;

            } catch (XMLStreamException e) {
                throwXmlReadError(e);
            }

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
