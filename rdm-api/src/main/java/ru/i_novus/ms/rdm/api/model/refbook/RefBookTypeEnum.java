package ru.i_novus.ms.rdm.api.model.refbook;

import java.util.HashMap;
import java.util.Map;

/**
 * Тип справочника.
 */
public enum RefBookTypeEnum {

    DEFAULT,        // По умолчанию
    UNVERSIONED     // Неверсионный
    ;

    private static final Map<String, RefBookTypeEnum> TYPE_MAP = new HashMap<>();
    static {
        for (RefBookTypeEnum type : RefBookTypeEnum.values()) {
           TYPE_MAP.put(type.name(), type);
        }
    }

    /**
     * Получение типа по строковому значению типа.
     * <p/>
     * Значение null может прийти из справочника без типа.
     * Такой справочник считается справочником с типом "По умолчанию".
     * Обычный {@link RefBookTypeEnum#valueOf} не подходит, т.к. кидает исключение.
     *
     * @param value Строковое значение
     * @return Тип справочника
     */
    public static RefBookTypeEnum fromValue(String value) {

        return value != null ? TYPE_MAP.get(value) : RefBookTypeEnum.DEFAULT;
    }
}
