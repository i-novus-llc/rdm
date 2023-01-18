package ru.i_novus.ms.rdm.n2o.model;

/**
 * Паспорт справочника для UI.
 */
public class UiPassport {

    /** Код справочника */
    private String code;

    /** Категория справочника */
    private String category;

    /** Наименование */
    private String name;
    /** Краткое наименование */
    private String shortName;
    /** Описание */
    private String description;

    public UiPassport() {
        // Nothing to do.
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
