package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

/**
 * Модель типа справочника для UI.
 */
public class UiRefBookType {

    /** Тип справочника. */
    private RefBookTypeEnum id;

    /** Наименование типа справочника. */
    private String name;

    @SuppressWarnings("unused")
    public UiRefBookType() {
    }

    public UiRefBookType(RefBookTypeEnum id, String name) {
        this.id = id;
        this.name = name;
    }

    public RefBookTypeEnum getId() {
        return id;
    }

    public void setId(RefBookTypeEnum id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
