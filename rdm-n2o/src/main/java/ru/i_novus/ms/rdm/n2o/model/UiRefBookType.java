package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;

/**
 * Тип справочника для списка.
 */
public class UiRefBookType {

    private RefBookType id;
    private String name;

    @SuppressWarnings("unused")
    public UiRefBookType() {
    }

    public UiRefBookType(RefBookType id, String name) {
        this.id = id;
        this.name = name;
    }

    public RefBookType getId() {
        return id;
    }

    public void setId(RefBookType id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
