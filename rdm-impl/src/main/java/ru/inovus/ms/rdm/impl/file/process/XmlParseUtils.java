package ru.inovus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

import static java.util.Arrays.asList;

public class XmlParseUtils {

    private static final Logger logger = LoggerFactory.getLogger(XmlParseUtils.class);

    private XmlParseUtils() {
    }

    public static void parseValues(XMLEventReader reader, Map<String, Object> map, String outerTagName) throws XMLStreamException {
        Deque<String> stack = new LinkedList<>();
        stack.push(outerTagName);
        parseValues(reader, map, outerTagName, stack);
    }

    private static void parseValues(XMLEventReader reader, Map<String, Object> map, String outerTagName, Deque<String> stack) throws XMLStreamException {
        String keyValue = null;
        XMLEvent curEvent = reader.nextEvent();
        while (curEvent != null && !isEndElementWithName(curEvent, outerTagName)) {
            if (curEvent.isStartElement()) {
                String curr = curEvent.asStartElement().getName().getLocalPart();
                if (!stack.peek().equals(outerTagName)) {
                    levelDown(curr, stack, reader, map);
                } else {
                    stack.push(curr);
                }
            } else if (curEvent.isCharacters()) {
                keyValue = curEvent.asCharacters().getData();
            } else if (curEvent.isEndElement()) {
                map.put(curEvent.asEndElement().getName().getLocalPart(), keyValue);
                keyValue = null;
                stack.pop();
            }
            curEvent = reader.nextEvent();
        }
    }

    private static void levelDown(String curr, Deque<String> stack, XMLEventReader reader, Map<String, Object> map) throws XMLStreamException {
        Map<String, Object> m = new HashMap<>();
        String outer = stack.peek();
        stack.push(curr);
        parseValues(reader, m, outer, stack);
        if (map.containsKey(outer)) {
            Object obj = map.get(outer);
            if (obj instanceof List)
                ((List) obj).add(m);
            else { // map
                List<Object> l = new ArrayList<>();
                l.add(map.get(outer));
                l.add(m);
                map.put(outer, l);
            }
        } else
            map.put(outer, m);
    }

    public static boolean isStartElementWithName(XMLEvent event, String... tagNames) {
        return event != null && event.isStartElement()
                && asList(tagNames).contains(event.asStartElement().getName().getLocalPart());
    }

    public static boolean isEndElementWithName(XMLEvent event, String... tagNames) {
        return event.isEndElement()
                && asList(tagNames).contains(event.asEndElement().getName().getLocalPart());
    }

    public static void throwXmlReadError(Exception e) {
        logger.error("Error while processing xml file.", e);
        throw new UserException("xml.processing.failed", e);
    }

}
