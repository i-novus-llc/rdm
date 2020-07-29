package ru.i_novus.ms.rdm.api.enumeration;

public enum RefBookOperation {

    PUBLISHING("Публикуется"),
    UPDATING("Обновляется");

    private String name;

    RefBookOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
