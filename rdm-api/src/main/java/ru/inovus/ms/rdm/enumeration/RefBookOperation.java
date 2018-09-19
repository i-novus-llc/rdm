package ru.inovus.ms.rdm.enumeration;

public enum RefBookOperation {

    PUBLISHING  ("Публикуется"),
    UPLOADING   ("Загружается");

    private String name;

    RefBookOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
