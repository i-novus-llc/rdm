package ru.inovus.ms.rdm.file;

import java.util.Map;

public class Row {

    private Map<String, Object> data;

    public Row(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
