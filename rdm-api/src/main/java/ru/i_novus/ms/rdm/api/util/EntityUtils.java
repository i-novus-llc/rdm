package ru.i_novus.ms.rdm.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toCollection;

/**
 * Класс для работы с сущностями.
 */
public final class EntityUtils {

    private EntityUtils() {
        // Nothing to do.
    }

    /**
     * Формирование списка сущностей из потока.
     * <p>
     * Создаёт изменяемый список для правильной работы Hibernate.
     * <p>
     * Пример: {@code list.stream().collect(toEntityList());}
     *
     * @return Изменяемый список
     * @param <T> Класс сущности
     */
    public static <T> Collector<T, ?, List<T>> toEntityList() {
        return toCollection(ArrayList::new);
    }
}
