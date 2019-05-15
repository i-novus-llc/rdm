package ru.inovus.ms.rdm.file.process;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class XmlParseUtils {

    private static final String XML_READ_ERROR_MESSAGE = "cannot read XML";

    private static final Logger logger = LoggerFactory.getLogger(XmlParseUtils.class);

    private XmlParseUtils() {
    }

    public static void parseValues(XMLEventReader reader, Map<String, String> map, String outerTagName) throws XMLStreamException {
        String keyValue = null;
        XMLEvent curEvent = reader.nextEvent();
        List<String> keyParts = new ArrayList<>();
        while (curEvent != null && !isEndElementWithName(curEvent, outerTagName)) {
            if (curEvent.isStartElement()) {
                keyParts.add(curEvent.asStartElement().getName().getLocalPart());
            } else if (curEvent.isCharacters()) {
                keyValue = curEvent.asCharacters().getData();
            } else if (curEvent.isEndElement()) {
                if (keyValue != null) {
                    map.put(getKey(keyParts), keyValue);
                }
                keyParts.remove(curEvent.asEndElement().getName().getLocalPart());
                keyValue = null;
            }
            curEvent = reader.nextEvent();
        }
    }

    public static boolean isStartElementWithName(XMLEvent event, String... tagNames) {
        return event != null && event.isStartElement()
                && asList(tagNames).contains(event.asStartElement().getName().getLocalPart());
    }

    public static boolean isEndElementWithName(XMLEvent event, String... tagNames) {
        return event.isEndElement()
                && asList(tagNames).contains(event.asEndElement().getName().getLocalPart());
    }

    private static String getKey(List<String> keyParts) {
        if (CollectionUtils.isEmpty(keyParts)) {
            return null;
        }

        String key = null;
        if(keyParts.size() == 1) {
            key = keyParts.get(0);
        } else {
            StringBuilder keyBuilder = new StringBuilder(keyParts.get(0));
            for (int i = 1; i < keyParts.size(); i++) {
                keyBuilder.append(".").append(keyParts.get(i));
            }
            key = keyBuilder.toString();
        }

        return key;

    }

    public static void throwXmlReadError(Exception e) {
        logger.error(XML_READ_ERROR_MESSAGE, e);
        throw new UserException(XML_READ_ERROR_MESSAGE);
    }
}
