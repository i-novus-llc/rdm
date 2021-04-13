package ru.i_novus.ms.rdm.api.model.refbook;

import java.util.HashMap;
import java.util.Map;

/**
 * Тип справочника.
 */
@SuppressWarnings("I-novus:EnumName")
public enum RefBookType {

    UNVERSIONED     // Неверсионный
    ;

    private static final Map<String, RefBookType> TYPE_MAP = new HashMap<>();
    static {
        for (RefBookType type : RefBookType.values()) {
           TYPE_MAP.put(type.name(), type);
        }
    }

    public static RefBookType fromValue(String value) {

        return value != null ? TYPE_MAP.get(value) : null;
    }
}
