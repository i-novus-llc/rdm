package ru.i_novus.ms.rdm.api.enumeration;

/**
 * Статус версии справочника:
 * - DRAFT      - версия-черновик - версия, в которой можно менять паспорт, структуру и данные.
 * - PUBLISHED  - опубликованная версия - версия с неизменяемым паспортом, структурой и данными.
 */
@SuppressWarnings("I-novus:EnumName")
public enum RefBookVersionStatus {

    DRAFT       ("Черновик"),
    PUBLISHED   ("Опубликован");

    private final String name;

    RefBookVersionStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
