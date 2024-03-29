package ru.i_novus.ms.rdm.n2o.model;

/**
 * Модель статуса справочника для UI.
 */
public class UiRefBookStatus {

    /** Статус справочника. */
    private RefBookStatus id;

    /** Локализованное наименование. */
    private String name;

    @SuppressWarnings("unused")
    public UiRefBookStatus() {
    }

    public UiRefBookStatus(RefBookStatus id, String name) {
        this.id = id;
        this.name = name;
    }

    public RefBookStatus getId() {
        return id;
    }

    public void setId(RefBookStatus id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
