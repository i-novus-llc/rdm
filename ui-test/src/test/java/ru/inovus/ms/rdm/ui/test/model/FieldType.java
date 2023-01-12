package ru.inovus.ms.rdm.ui.test.model;

/**
 * Представление типа поля с локальным наименованием.
 */
public enum FieldType {

    STRING("Строковый"),
    INTEGER("Целочисленный"),
    DOUBLE("Дробный"),
    BOOLEAN("Логический"),
    REFERENCE("Ссылочный"),
    DATE("Дата");

    private final String label;

    FieldType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
