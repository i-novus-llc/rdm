package ru.inovus.ms.rdm.file;

import java.util.LinkedHashMap;

public class Row {

    private LinkedHashMap<String, Object> data;

    public Row(LinkedHashMap<String, Object> data) {
        this.data = data;
    }

    public LinkedHashMap<String, Object> getData() {
        return data;
    }
}
