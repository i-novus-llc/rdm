package ru.inovus.ms.rdm.enumeration;

public enum RefBookVersionStatus {

    DRAFT       ("Черновик"),
    PUBLISHING  ("Публикуется"),
    PUBLISHED   ("Опубликован");

    private String name;

    RefBookVersionStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
