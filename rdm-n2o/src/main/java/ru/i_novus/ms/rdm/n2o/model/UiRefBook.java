package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.model.refbook.RefBook;

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

        this.displayNumber = uiRefBook.getDisplayNumber();
        this.displayStatus = uiRefBook.getDisplayStatus();
        this.displayOperation = uiRefBook.getDisplayOperation();
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
}
