package ru.inovus.ms.rdm.ui.test.model;

public class RefBook {

    private final String code;
    private final String name;
    private final String shortName;
    private final String category;
    private final String description;

    public RefBook(String code, String name, String shortName, String category, String description) {
        this.code = code;
        this.name = name;
        this.shortName = shortName;
        this.category = category;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
}
