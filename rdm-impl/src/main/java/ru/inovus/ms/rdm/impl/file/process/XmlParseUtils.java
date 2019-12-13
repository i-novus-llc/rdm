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

    /**
     * Спарсить значения из XML-ки. В мапе будут ключом будут идти имена тегов, а значениями:
     * 1) String -- в случае, если тег открылся, в нем текст и тег закрылся
     * 2) Map<String, Object> -- в случае, если под тегом вложенный объект (для него все те же правила применяются рекурсивно)
     * 3) List<Object> -- в случае, если несколько тегов с одним ключом в пределах одного уровня вложенности. Тут точно так же могу лежать либо просто String, либо Map<String, Object>
     * @param map Куда складывать значения
     * @param outerTagName Открывающий тег (этот тег не должен встречаться в пределах одного уровня вложенности)
     */
    public static void parseValues(XMLEventReader reader, Map<String, Object> map, String outerTagName) throws XMLStreamException {
        Deque<String> stack = new LinkedList<>();
        stack.push(outerTagName);
        parseValues(reader, map, outerTagName, stack);
    }

    private static void parseValues(XMLEventReader reader, Map<String, Object> map, String outerTagName, Deque<String> stack) throws XMLStreamException {
        String val = null;
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
                val = curEvent.asCharacters().getData();
            } else if (curEvent.isEndElement()) {
                String curr = curEvent.asEndElement().getName().getLocalPart();
                add(map, curr, val);
                val = null;
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
        add(map, outer, m);
    }

    private static void add(Map<String, Object> m, String key, Object val) {
        m.compute(key, (k, v) -> {
            if (v == null) return val;
            else {
                if (v instanceof List) {
                    ((List) v).add(val);
                    return v;
                } else {
                    List<Object> arr = new ArrayList<>();
                    arr.add(v);
                    arr.add(val);
                    return arr;
                }
            }
        });
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
