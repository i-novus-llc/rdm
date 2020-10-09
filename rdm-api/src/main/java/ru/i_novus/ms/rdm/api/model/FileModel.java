package ru.i_novus.ms.rdm.api.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;

@ApiModel(value = "Модель сохранённого файла",
        description = "Набор входных параметров для сохранённого файла")
public class FileModel implements Serializable {

    private static final String SEPARATOR = "/";

    @ApiModelProperty("Полный путь к файлу")
    private String path;

    @ApiModelProperty("Наименование файла")
    private String name;

    public FileModel() {
        // Nothing to do.
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
        return new StringBuilder()
                .append(calendar.get(Calendar.YEAR)).append(SEPARATOR)
                .append(calendar.get(Calendar.MONTH) + 1).append(SEPARATOR)
                .append(calendar.get(Calendar.DATE)).append(SEPARATOR)
                .append(calendar.get(Calendar.HOUR_OF_DAY)).append(SEPARATOR)
                .append(calendar.get(Calendar.MINUTE)).append(SEPARATOR)
                .append(calendar.get(Calendar.SECOND)).append(SEPARATOR)
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
        return JsonUtil.toJsonString(this);
    }
}
