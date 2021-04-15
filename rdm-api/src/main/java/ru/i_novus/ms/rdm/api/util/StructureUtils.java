package ru.i_novus.ms.rdm.api.util;

import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;

public class StructureUtils {

    private StructureUtils() {
        throw new UnsupportedOperationException();
    }

    /** Проверка на наличие атрибута-ссылки. */
    public static boolean isReference(Structure.Reference reference) {
        return reference != null && !reference.isNull();
    }

    /** Сравнение displayExpression двух ссылок. */
    public static boolean isDisplayExpressionEquals(Structure.Reference reference1,
                                                    Structure.Reference reference2) {
        return reference1 != null && reference2 != null
                && Objects.equals(reference1.getDisplayExpression(), reference2.getDisplayExpression());
    }

    /**
     * Проверка на наличие хотя бы одного placeholder`а в выражении.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param placeholders      список проверяемых подставляемых значений
     * @return Наличие
     */
    public static boolean containsAnyPlaceholder(String displayExpression, List<String> placeholders) {

        if (isEmpty(displayExpression) || isEmpty(placeholders))
            return false;

        DisplayExpression expression = new DisplayExpression(displayExpression);
        return CollectionUtils.containsAny(expression.getPlaceholders().keySet(), placeholders);
    }

    /**
     * Проверка полей выражения на отсутствие в структуре.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param structure         структура версии, на которую ссылаются
     * @return Признак отсутствия
     */
    public static boolean hasAbsentPlaceholder(String displayExpression, Structure structure) {

        if (isEmpty(displayExpression))
            return false;

        DisplayExpression expression = new DisplayExpression(displayExpression);
        return expression.getPlaceholders().keySet().stream()
                .anyMatch(placeholder -> structure.getAttribute(placeholder) == null);
    }

    /**
     * Поиск полей выражения, которые отсутствуют в структуре.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param structure         структура версии, на которую ссылаются
     * @return Список отсутствующих полей
     */
    public static List<String> getAbsentPlaceholders(String displayExpression, Structure structure) {

        if (isEmpty(displayExpression))
            return emptyList();

        DisplayExpression expression = new DisplayExpression(displayExpression);
        return expression.getPlaceholders().keySet().stream()
                .filter(placeholder -> structure.getAttribute(placeholder) == null)
                .collect(toList());
    }

    /**
     * Получение placeholder`а из выражения (если нет других placeholder`ов).
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @return Код атрибута
     */
    public static String displayExpressionToPlaceholder(String displayExpression) {

        if (isEmpty(displayExpression))
            return null;

        DisplayExpression expression = new DisplayExpression(displayExpression);
        Collection<String> placeholders = expression.getPlaceholders().keySet();
        if (placeholders.size() == 1) {

            String placeholder = placeholders.iterator().next();
            if (DisplayExpression.toPlaceholder(placeholder).equals(displayExpression))
                return placeholder;
        }
        return null;
    }
}
