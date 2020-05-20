package ru.inovus.ms.rdm.impl.file.process;

import org.springframework.data.util.Pair;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.api.service.DraftService;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.InputStream;
import java.util.*;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.impl.file.process.FileParseUtils.throwFileContentError;
import static ru.inovus.ms.rdm.impl.file.process.XmlParseUtils.createEvenReader;

public class XmlUpdateDraftFileProcessor extends UpdateDraftFileProcessor implements Closeable {

    private static final String PASSPORT_TAG_NAME = "passport";
    private static final String STRUCTURE_TAG_NAME = "structure";
    private static final String DATA_TAG_NAME = "data";
    private static final String ROW_TAG_NAME = "row";

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private XMLEventReader reader;

    private Map<String, Object> passport;

    private boolean passportProcessed = false;

    public XmlUpdateDraftFileProcessor(Integer refBookId, DraftService draftService) {
        super(refBookId, draftService);
    }

    @Override
    protected void setFile(InputStream inputStream) {
        reader = createEvenReader(inputStream, FACTORY);
    }

    @Override
    public Map<String, Object> getPassport(){
        if(passportProcessed) {
            return passport;
        }
        try {
            if (reader.hasNext()) {
                if (reader.peek().isStartDocument())
                    reader.nextEvent();

                XMLEvent curEvent = null;
                while (reader.peek() != null && !(XmlParseUtils.isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, STRUCTURE_TAG_NAME, DATA_TAG_NAME))) {
                    curEvent = reader.nextEvent();
                }
                if (curEvent == null || reader.peek() == null || XmlParseUtils.isStartElementWithName(reader.peek(), STRUCTURE_TAG_NAME) || XmlParseUtils.isStartElementWithName(reader.peek(), DATA_TAG_NAME))
                    return null;

                passport = new LinkedHashMap<>();
                reader.nextEvent();     // current is start-tag <passport>
                XmlParseUtils.parseValues(reader, passport, PASSPORT_TAG_NAME);
            }
        } catch (XMLStreamException e) {
            throwFileContentError(e);
        }
        passportProcessed = true;
        return passport;
    }

    private void parseStructureAndValidations(Structure structure, Map<String, List<AttributeValidation>> validations) throws XMLStreamException {
        reader.nextEvent();
        while (!XmlParseUtils.isEndElementWithName(reader.peek(), STRUCTURE_TAG_NAME) &&
                !XmlParseUtils.isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME)) {
            Map<String, Object> attribute = new LinkedHashMap<>();
            reader.nextEvent();// current is <row> in <structure>

            XmlParseUtils.parseValues(reader, attribute, ROW_TAG_NAME);
            Structure.Attribute structureAttribute = new Structure.Attribute();

            String code = (String) attribute.get("code");
            structureAttribute.setCode(code);
            structureAttribute.setDescription((String) attribute.get("description"));
            structureAttribute.setName((String) attribute.get("name"));
            structureAttribute.setType(FieldType.valueOf((String) attribute.get("type")));
            structureAttribute.setPrimary(Boolean.valueOf((String) attribute.get("primary")));
            structure.getAttributes().add(structureAttribute);

            if(FieldType.REFERENCE.equals(structureAttribute.getType())) {
                String referenceCode = (String) attribute.get("referenceCode");
                if(referenceCode != null) {
                    Structure.Reference structureReference = new Structure.Reference();
                    structureReference.setAttribute(structureAttribute.getCode());
                    structureReference.setReferenceCode(referenceCode);
                    structureReference.setDisplayExpression((String) attribute.get("displayExpression"));
                    structure.getReferences().add(structureReference);
                }
            }

            Object obj = attribute.get("validation");
            if (obj == null)
                continue;
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                AttributeValidation validation = AttributeValidation.of((String) map.get("type"), (String) map.get("value"));
                validations.put(code, singletonList(validation));
            } else { // List
                groupValidationsByCode(code, (List<Map<String, Object>>) obj, validations);
            }
        }
    }

    private void groupValidationsByCode(String code, List<Map<String, Object>> list, Map<String, List<AttributeValidation>> validations) {
        for (Map<String, Object> map : list) {
            AttributeValidation validation = AttributeValidation.of((String) map.get("type"), (String) map.get("value"));
            if (!validations.containsKey(code))
                validations.put(code, new ArrayList<>());
            validations.get(code).add(validation);
        }
    }

    @Override
    protected Pair<Structure, Map<String, List<AttributeValidation>>> getStructureAndValidations() {
        if (reader.hasNext()) {
            try {
                XMLEvent curEvent = reader.peek();
                while (curEvent != null && !(XmlParseUtils.isStartElementWithName(curEvent, STRUCTURE_TAG_NAME, DATA_TAG_NAME))) {
                    reader.nextEvent();
                    curEvent = reader.peek();
                }

                if (curEvent == null || reader.peek() == null ||
                        XmlParseUtils.isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME))
                    return null;

                Structure structure = new Structure(new ArrayList<>(), new ArrayList<>());
                Map<String, List<AttributeValidation>> validations = new HashMap<>();
                parseStructureAndValidations(structure, validations);

                reader.nextTag();

                return Pair.of(structure, validations);

            } catch (XMLStreamException e) {
                throwFileContentError(e);
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
                throwFileContentError(e);
            }
        }
    }
}
