package ru.inovus.ms.rdm.n2o.util;

import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.inovus.ms.rdm.n2o.model.Structure;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;

public class StructureUtils {

    private StructureUtils() {
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
        return CollectionUtils.containsAny(expression.getPlaceholders(), placeholders);
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
        return expression.getPlaceholders().stream()
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
        List<String> placeholders = expression.getPlaceholders();
        if (Objects.nonNull(placeholders) && placeholders.size() == 1) {
            String placeholder = placeholders.get(0);
            if (DisplayExpression.toPlaceholder(placeholder).equals(displayExpression)) {
                return placeholder;
            }
        }
        return null;
    }
}
