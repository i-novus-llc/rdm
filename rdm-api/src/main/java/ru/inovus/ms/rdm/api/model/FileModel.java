package ru.inovus.ms.rdm.api.model;

import java.util.Calendar;

public class FileModel {
    private String path;
    private String name;

    public FileModel() {
    }

    public FileModel(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String generateFullPath() {
        Calendar calendar = Calendar.getInstance();
        String separator = "/";
        return new StringBuilder()
                .append(calendar.get(Calendar.YEAR)).append(separator)
                .append(calendar.get(Calendar.MONTH) + 1).append(separator)
                .append(calendar.get(Calendar.DATE)).append(separator)
                .append(calendar.get(Calendar.HOUR_OF_DAY)).append(separator)
                .append(calendar.get(Calendar.MINUTE)).append(separator)
                .append(calendar.get(Calendar.SECOND)).append(separator)
                .append(name)
                .toString();
    }

}
