package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Objects;

/**
 * Модель справочника для UI.
 * <p/>
 * Содержит поля, необходимые для вывода локализованных значений.
 */
public class UiRefBook extends RefBook {

    /** Наименование типа. */
    private String typeName;

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

        this.typeName = uiRefBook.typeName;

        this.displayNumber = uiRefBook.displayNumber;
        this.displayStatus = uiRefBook.displayStatus;
        this.displayOperation = uiRefBook.displayOperation;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
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
        return Objects.equals(typeName, that.typeName) &&

                Objects.equals(displayNumber, that.displayNumber) &&
                Objects.equals(displayStatus, that.displayStatus) &&
                Objects.equals(displayOperation, that.displayOperation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), typeName, displayNumber, displayStatus, displayOperation);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + JsonUtil.toJsonString(this);
    }
}
