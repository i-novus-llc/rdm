package ru.i_novus.ms.rdm.api.model;

import java.util.List;

public class ExistsData {

    private boolean exists;

    private List<String> notExistingRowIds;

    public ExistsData() {
    }

    public ExistsData(boolean exists, List<String> notExistingRowIds) {
        this.exists = exists;
        this.notExistingRowIds = notExistingRowIds;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public List<String> getNotExistingRowIds() {
        return notExistingRowIds;
    }

    public void setNotExistingRowIds(List<String> notExistingRowIds) {
        this.notExistingRowIds = notExistingRowIds;
    }
}
