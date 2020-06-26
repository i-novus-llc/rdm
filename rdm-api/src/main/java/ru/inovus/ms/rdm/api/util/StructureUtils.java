package ru.inovus.ms.rdm.api.util;

import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.inovus.ms.rdm.api.model.Structure;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;

public class StructureUtils {

    private StructureUtils() {
    }

    /** Получение кодов атрибутов структуры. */
    public static Stream<String> getAttributeCodes(Structure structure) {
        return structure.getAttributes().stream().map(Structure.Attribute::getCode);
    }

    /** Получение кодов атрибутов-ссылок структуры. */
    public static Stream<String> getReferenceAttributeCodes(Structure structure) {
        return structure.getReferences().stream().map(Structure.Reference::getAttribute);
    }

    /** Сравнение displayExpression двух ссылок. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
                .anyMatch(placeholder -> Objects.isNull(structure.getAttribute(placeholder)));
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
                .filter(placeholder -> Objects.isNull(structure.getAttribute(placeholder)))
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
            if (DisplayExpression.toPlaceholder(placeholder).equals(displayExpression)) {
                return placeholder;
            }
        }
        return null;
    }
}
