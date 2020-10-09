package ru.i_novus.ms.rdm.loader.client;

import org.springframework.core.io.Resource;

import java.util.Objects;

/** Модель загрузки справочника. */
public class RefBookDataModel {

    /** Код справочника. */
    private String code;

    /** Название справочника. */
    private String name;

    /** Структура справочника в формате json. */
    private String structure;

    /** Записи справочника в формате json. */
    private String data;

    /** Файл справочника. */
    private Resource file;

    public RefBookDataModel() {
        // Nothing to do.
    }

    public RefBookDataModel(String code, String name, String structure, Resource file) {

        this.code = code;
        this.name = name;
        this.structure = structure;
        this.file = file;
    }

    public RefBookDataModel(String code, String name, String structure, String data) {

        this.code = code;
        this.name = name;
        this.structure = structure;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Resource getFile() {
        return file;
    }

    public void setFile(Resource file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefBookDataModel)) return false;

        RefBookDataModel that = (RefBookDataModel) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(structure, that.structure) &&
                Objects.equals(data, that.data) &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {

        return Objects.hash(code, name, structure, data, file);
    }
}
