package ru.i_novus.ms.rdm.impl.file.process;

import ru.i_novus.ms.rdm.api.exception.FileContentException;
import ru.i_novus.ms.rdm.api.exception.FileProcessingException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.*;

import static java.util.Arrays.asList;

public class XmlParseUtils {

    private XmlParseUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Создание считывателя для парсинга xml.
     *
     * @param inputStream входной поток xml
     * @param factory     фабрика для создания считывателя
     * @return Считыватель xml
     */
    public static XMLEventReader createEventReader(InputStream inputStream, XMLInputFactory factory) {
        try {
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            XMLEventReader simpleReader = factory.createXMLEventReader(inputStream);
            return factory.createFilteredReader(simpleReader,
                    event -> !(event.isCharacters() && event.asCharacters().isWhiteSpace())
            );
        } catch (XMLStreamException e) {
            throw new FileContentException(e);
        }
    }

    /**
     * Закрытие считывателя для парсинга xml.
     *
     * @param reader считыватель xml
     */
    public static void closeEventReader(XMLEventReader reader) {

        if (reader == null)
            return;

        try {
            reader.close();

        } catch (XMLStreamException e) {
            throw new FileProcessingException(e);
        }
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

    /**
     * Проверка события на наличие наименования начального элемента в списке тегов.
     *
     * @param event    xml-событие
     * @param tagNames список проверяемых тегов
     * @return Результат проверки
     */
    public static boolean isStartElementWithName(XMLEvent event, String... tagNames) {
        return event != null && event.isStartElement()
                && asList(tagNames).contains(event.asStartElement().getName().getLocalPart());
    }

    /**
     * Проверка события на наличие наименования конечного элемента в списке тегов.
     *
     * @param event    xml-событие
     * @param tagNames список проверяемых тегов
     * @return Результат проверки
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isEndElementWithName(XMLEvent event, String... tagNames) {
        return event.isEndElement()
                && asList(tagNames).contains(event.asEndElement().getName().getLocalPart());
    }

    /**
     * Поиск события с наименованием начального элемента из списка тегов.
     *
     * @param reader   xml-reader
     * @param tagNames список тегов
     * @return Найденное событие или null
     */
    public static XMLEvent findStartElementWithName(XMLEventReader reader, String... tagNames) throws XMLStreamException {

        XMLEvent event = reader.peek();
        while (event != null &&
                !(isStartElementWithName(event, tagNames))) {
            if (!reader.hasNext())
                return null;

            reader.nextEvent();
            event = reader.peek(); // after nextEvent!
        }
        return event;
    }
}
