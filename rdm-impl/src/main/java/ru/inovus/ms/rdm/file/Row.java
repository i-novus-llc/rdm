package ru.inovus.ms.rdm.file;

import java.util.LinkedHashMap;
import java.util.Map;

public class Row {

    private Map<String, Object> data = new LinkedHashMap<>();

    public Row(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
