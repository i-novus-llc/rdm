package ru.i_novus.ms.rdm.api.model.loader;

import java.util.HashMap;
import java.util.Map;

/**
 * Тип изменения справочника.
 */
public enum RefBookDataUpdateTypeEnum {

    CREATE_ONLY,
    FORCE_UPDATE,
    SKIP_ON_DRAFT
    ;

    private static final Map<String, RefBookDataUpdateTypeEnum> TYPE_MAP = new HashMap<>();
    static {
        for (RefBookDataUpdateTypeEnum type : RefBookDataUpdateTypeEnum.values()) {
            TYPE_MAP.put(type.name().toLowerCase(), type);
        }
    }

    /**
     * Получение типа по строковому значению типа.
     * Строковым значением является наименование типа в нижнем регистре.
     * Обычный {@link RefBookDataUpdateTypeEnum#valueOf} не подходит, т.к. кидает исключение.
     *
     * @param value Строковое значение
     * @return Тип справочника
     */
    public static RefBookDataUpdateTypeEnum fromValue(String value, RefBookDataUpdateTypeEnum defaultType) {

        return value != null ? TYPE_MAP.get(value) : defaultType;
    }
}
