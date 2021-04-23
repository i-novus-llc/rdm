package ru.i_novus.ms.rdm.impl.file.process;

import org.springframework.data.util.Pair;
import ru.i_novus.ms.rdm.api.exception.FileContentException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.InputStream;
import java.util.*;

import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.impl.file.process.XmlParseUtils.closeEventReader;
import static ru.i_novus.ms.rdm.impl.file.process.XmlParseUtils.createEventReader;

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
        reader = createEventReader(inputStream, FACTORY);
    }

    @Override
    public Map<String, Object> getPassport(){

        if (passportProcessed)
            return passport;

        try {
            if (reader.hasNext()) {
                if (reader.peek().isStartDocument())
                    reader.nextEvent();

                XMLEvent curEvent = null;
                while (reader.peek() != null &&
                        !(XmlParseUtils.isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, STRUCTURE_TAG_NAME, DATA_TAG_NAME))) {
                    curEvent = reader.nextEvent();
                }
                if (curEvent == null || reader.peek() == null ||
                        XmlParseUtils.isStartElementWithName(reader.peek(), STRUCTURE_TAG_NAME) ||
                        XmlParseUtils.isStartElementWithName(reader.peek(), DATA_TAG_NAME))
                    return null;

                passport = new LinkedHashMap<>();
                reader.nextEvent();     // current is start-tag <passport>
                XmlParseUtils.parseValues(reader, passport, PASSPORT_TAG_NAME);
            }
        } catch (XMLStreamException e) {
            throw new FileContentException(e);
        }
        passportProcessed = true;

        return passport;
    }

    @SuppressWarnings("unchecked")
    private void parseStructureAndValidations(Structure structure,
                                              Map<String, List<AttributeValidation>> validations) throws XMLStreamException {
        reader.nextEvent();
        while (!XmlParseUtils.isEndElementWithName(reader.peek(), STRUCTURE_TAG_NAME) &&
                !XmlParseUtils.isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME)) {

            Map<String, Object> values = new LinkedHashMap<>();
            reader.nextEvent(); // current is <row> in <structure>

            XmlParseUtils.parseValues(reader, values, ROW_TAG_NAME);

            Structure.Attribute attribute = parseStructureAttribute(values);
            Structure.Reference reference = parseStructureReference(attribute, values);
            structure.add(attribute, reference);

            Object obj = values.get("validation");
            if (obj == null)
                continue;

            if (obj instanceof Map) {

                Map<String, Object> map = (Map<String, Object>) obj;
                AttributeValidation validation = AttributeValidation.of((String) map.get("type"), (String) map.get("value"));
                validations.put(attribute.getCode(), singletonList(validation));

            } else { // List
                groupValidationsByCode(attribute.getCode(), (List<Map<String, Object>>) obj, validations);
            }
        }
    }

    private Structure.Attribute parseStructureAttribute(Map<String, Object> values) {

        Structure.Attribute result = new Structure.Attribute();

        result.setCode((String) values.get("code"));
        result.setName((String) values.get("name"));
        result.setType(FieldType.valueOf((String) values.get("type")));

        result.setIsPrimary(Boolean.valueOf((String) values.get("primary")));
        result.setLocalizable(Boolean.valueOf((String) values.get("localizable")));
        result.setDescription((String) values.get("description"));

        return result;
    }

    private Structure.Reference parseStructureReference(Structure.Attribute attribute, Map<String, Object> values) {

        if (!attribute.isReferenceType())
            return null;

        String referenceCode = (String) values.get("referenceCode");
        if (referenceCode == null)
            return null;

        Structure.Reference result = new Structure.Reference();

        result.setAttribute(attribute.getCode());
        result.setReferenceCode(referenceCode);
        result.setDisplayExpression((String) values.get("displayExpression"));

        return result;
    }

    private void groupValidationsByCode(String code, List<Map<String, Object>> list,
                                        Map<String, List<AttributeValidation>> validations) {
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
                while (curEvent != null &&
                        !(XmlParseUtils.isStartElementWithName(curEvent, STRUCTURE_TAG_NAME, DATA_TAG_NAME))) {
                    reader.nextEvent();
                    curEvent = reader.peek();
                }

                if (curEvent == null || reader.peek() == null ||
                        XmlParseUtils.isStartElementWithName(reader.peek(), PASSPORT_TAG_NAME, DATA_TAG_NAME))
                    return null;

                Structure structure = new Structure();
                Map<String, List<AttributeValidation>> validations = new HashMap<>();
                parseStructureAndValidations(structure, validations);

                reader.nextTag();

                return Pair.of(structure, validations);

            } catch (XMLStreamException e) {
                throw new FileContentException(e);
            }
        }
        return null;
    }

    @Override
    public void close() {
        closeEventReader(reader);
    }
}
