package ru.inovus.ms.rdm.ui.test.model;

public enum FieldType {

    STRING("Строковый"),
    INTEGER("Целочисленный"),
    DOUBLE("Дробный"),
    BOOLEAN("Логический"),
    LINKED("Ссылочный"),
    DATE("Дата");

    private final String translated;

    FieldType(String translated) {
        this.translated = translated;
    }

    public String getTranslated() {
        return translated;
    }
}
