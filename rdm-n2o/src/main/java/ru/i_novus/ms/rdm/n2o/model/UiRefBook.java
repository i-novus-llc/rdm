package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.enumeration.RefBookOperation;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;

/**
 * Информация о справочнике для UI.
 */
public class UiRefBook extends RefBook {

    /**
     * Признак нахождения справочника в состоянии публикации.
     */
    private Boolean publishing;

    /**
     * Признак нахождения справочника в состоянии обновления.
     */
    private Boolean updating;

    public UiRefBook(RefBook refBook) {
        super(refBook);
    }

    public Boolean getPublishing() {
        return publishing;
    }

    public void setPublishing(Boolean publishing) {
        this.publishing = publishing;
    }

    public Boolean getUpdating() {
        return updating;
    }

    public void setUpdating(Boolean updating) {
        this.updating = updating;
    }

    /**
     * Проверка на операцию, выполняемую над справочником.
     *
     * @param operation операция
     * @return Результат проверки
     */
    public boolean isOperation(RefBookOperation operation) {

        return getCurrentOperation() != null && operation.equals(getCurrentOperation());
    }
}
