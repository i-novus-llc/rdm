package ru.inovus.ms.rdm.api.model.refdata;

import java.util.List;

public class ChangeDataRequest {

//  Выставляем systemId, если он известен. Если нет -- система попытается найти systemId по первичному ключу.
    private List<Row> rowsToAddOrUpdate;
    private List<Row> rowsToDelete;

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
