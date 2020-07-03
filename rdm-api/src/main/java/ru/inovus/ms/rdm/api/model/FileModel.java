package ru.inovus.ms.rdm.api.model;

import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;

public class FileModel implements Serializable {

    /** Полный путь к файлу. */
    private String path;

    /** Название файла. */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileModel that = (FileModel) o;
        return Objects.equals(path, that.path) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name);
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
