package ru.i_novus.ms.rdm.api.enumeration;

/**
 * Операция, выполняемая над справочником.
 */
@SuppressWarnings("I-novus:EnumName")
public enum RefBookOperation {

    PUBLISHING("Публикуется"),
    UPDATING("Обновляется");

    private final String name;

    RefBookOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
