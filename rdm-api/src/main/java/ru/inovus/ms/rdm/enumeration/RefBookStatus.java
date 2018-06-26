package ru.inovus.ms.rdm.enumeration;

public enum RefBookStatus {

    PUBLISHED   ("Опубликован"),
    DRAFT       ("Черновик"),
    ARCHIVED    ("Архив");

    private String name;

    RefBookStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
