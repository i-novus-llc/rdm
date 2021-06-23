package ru.inovus.ms.rdm.ui.test.model;

import java.util.List;
import java.util.Map;

public class RefBook {

    private static final String REFBOOK_UNVERSIONED = "Неверсионный";

    private final String code;
    private final String name;
    private final String shortName;
    private final String category;
    private final String description;
    private final String type;
    private final List<Map<RefBookField, Object>> rows;

    public RefBook(String code, String name, String shortName, String category, String description, String type, List<Map<RefBookField, Object>> rows) {
        this.code = code;
        this.name = name;
        this.shortName = shortName;
        this.category = category;
        this.description = description;
        this.type = type;
        this.rows = rows;
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

    public String getType() {
        return type;
    }

    public List<Map<RefBookField, Object>> getRows() {
        return rows;
    }

    public boolean isUnversioned() {
        return getUnversionedType().equals(getType());
    }

    public static String getUnversionedType() {
        return REFBOOK_UNVERSIONED;
    }
}
