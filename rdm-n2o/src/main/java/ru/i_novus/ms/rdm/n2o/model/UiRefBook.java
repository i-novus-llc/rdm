package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Objects;

/**
 * Информация о справочнике для UI.
 */
public class UiRefBook extends RefBook {

    /** Отображаемый номер версии. */
    private String displayNumber;

    /** Отображаемый статус. */
    private String displayStatus;

    /** Отображаемое наименование операции. */
    private String displayOperation;

    public UiRefBook(RefBook refBook) {
        super(refBook);
    }

    public UiRefBook(UiRefBook uiRefBook) {

        this((RefBook) uiRefBook);

        this.displayNumber = uiRefBook.displayNumber;
        this.displayStatus = uiRefBook.displayStatus;
        this.displayOperation = uiRefBook.displayOperation;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    public void setDisplayNumber(String displayNumber) {
        this.displayNumber = displayNumber;
    }

    public String getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(String displayStatus) {
        this.displayStatus = displayStatus;
    }

    public String getDisplayOperation() {
        return displayOperation;
    }

    public void setDisplayOperation(String displayOperation) {
        this.displayOperation = displayOperation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UiRefBook that = (UiRefBook) o;
        return Objects.equals(displayNumber, that.displayNumber) &&
                Objects.equals(displayStatus, that.displayStatus) &&
                Objects.equals(displayOperation, that.displayOperation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), displayNumber, displayStatus, displayOperation);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
