package ru.inovus.ms.rdm.ui.test.login;

public enum FieldType {

    STRING("Строковый"),
    INTEGER("Целочисленный"),
    DOUBLE("Дробный"),
    DATE("Дата"),
    BOOLEAN("Логический");

    private final String translated;

    FieldType(String translated) {
        this.translated = translated;
    }

    public String getTranslated() {
        return translated;
    }
}
