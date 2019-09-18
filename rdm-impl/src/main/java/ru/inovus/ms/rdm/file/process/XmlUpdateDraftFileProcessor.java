package ru.inovus.ms.rdm.file.process;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.service.api.DraftService;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static ru.inovus.ms.rdm.file.process.XmlParseUtils.*;
import static ru.inovus.ms.rdm.file.process.XmlParseUtils.isStartElementWithName;

public class XmlUpdateDraftFileProcessor extends UpdateDraftFileProcessor implements Closeable {

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

    private void parseStructure(Structure structure) throws XMLStreamException {
        if (structure == null)
            return;

        reader.nextEvent();
        while (!isEndElementWithName(reader.peek(), STRUCTURE_TAG_NAME) &&
                !isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME)) {
            Map<String, String> attribute = new LinkedHashMap<>();
            reader.nextEvent();// current is <row> in <structure>

            parseValues(reader, attribute, ROW_TAG_NAME);
            Structure.Attribute structureAttribute = new Structure.Attribute();

            structureAttribute.setCode(attribute.get("code"));
            structureAttribute.setDescription(attribute.get("description"));
            structureAttribute.setName(attribute.get("name"));
            structureAttribute.setType(FieldType.valueOf(attribute.get("type")));
            structureAttribute.setPrimary(Boolean.valueOf(attribute.get("primary")));
            structure.getAttributes().add(structureAttribute);

            if(FieldType.REFERENCE.equals(structureAttribute.getType())) {
                String referenceCode = attribute.get("referenceCode");
                if(referenceCode != null) {
                    Structure.Reference structureReference = new Structure.Reference();
                    structureReference.setAttribute(structureAttribute.getCode());
                    structureReference.setReferenceCode(referenceCode);
                    structureReference.setDisplayExpression(attribute.get("displayExpression"));
                    structure.getReferences().add(structureReference);
                }
            }
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

                if (curEvent == null || reader.peek() == null ||
                        isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME))
                    return null;

                Structure structure = new Structure(new ArrayList(), new ArrayList<>());
                parseStructure(structure);

                reader.nextTag();

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
