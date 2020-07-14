package ru.i_novus.ms.rdm.api.model.refdata;

import java.io.Serializable;
import java.util.List;

public class RdmChangeDataRequest implements Serializable {

//  Выставляем systemId, если он известен. Если нет -- система попытается найти systemId по первичному ключу.
    private List<Row> rowsToAddOrUpdate;
    private List<Row> rowsToDelete;

    private String refBookCode;

    public RdmChangeDataRequest() {}

    public RdmChangeDataRequest(String refBookCode, List<Row> rowsToAddOrUpdate, List<Row> rowsToDelete) {
        this.rowsToAddOrUpdate = rowsToAddOrUpdate;
        this.rowsToDelete = rowsToDelete;
        this.refBookCode = refBookCode;
    }

    public String getRefBookCode() {
        return refBookCode;
    }

    public void setRefBookCode(String refBookCode) {
        this.refBookCode = refBookCode;
    }

    public List<Row> getRowsToAddOrUpdate() {
        return rowsToAddOrUpdate;
    }

    public void setRowsToAddOrUpdate(List<Row> rowsToAddOrUpdate) {
        this.rowsToAddOrUpdate = rowsToAddOrUpdate;
    }

    public List<Row> getRowsToDelete() {
        return rowsToDelete;
    }

    public void setRowsToDelete(List<Row> rowsToDelete) {
        this.rowsToDelete = rowsToDelete;
    }

}
