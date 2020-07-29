package ru.i_novus.ms.rdm.api.enumeration;

public enum RefBookVersionStatus {

    DRAFT       ("Черновик"),
    PUBLISHED   ("Опубликован");

    private String name;

    RefBookVersionStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
