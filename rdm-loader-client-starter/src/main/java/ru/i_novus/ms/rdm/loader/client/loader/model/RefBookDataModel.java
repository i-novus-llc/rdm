package ru.i_novus.ms.rdm.loader.client.loader.model;

import org.springframework.core.io.Resource;

import java.util.Objects;

/** Модель загрузки справочника. */
public class RefBookDataModel {

    /** Идентификатор изменения справочника. */
    private String changeSetId;

    /** Тип изменения справочника. */
    private RefBookDataUpdateTypeEnum updateType = RefBookDataUpdateTypeEnum.CREATE_ONLY;

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

    public RefBookDataModel(Resource file) {
        this.file = file;
    }

    public String getChangeSetId() {
        return changeSetId;
    }

    public void setChangeSetId(String changeSetId) {
        this.changeSetId = changeSetId;
    }

    public RefBookDataUpdateTypeEnum getUpdateType() {
        return updateType;
    }

    public void setUpdateType(RefBookDataUpdateTypeEnum updateType) {
        this.updateType = updateType;
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
        if (o == null || getClass() != o.getClass()) return false;

        final RefBookDataModel that = (RefBookDataModel) o;
        return Objects.equals(changeSetId, that.changeSetId) &&
                (updateType == that.updateType) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(structure, that.structure) &&
                Objects.equals(data, that.data) &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeSetId, updateType, code, name, structure, data, file);
    }

    @Override
    public String toString() {
        return "RefBookDataModel{" +
                (changeSetId != null ? "changeSetId='" + changeSetId + '\'' : "") +
                (updateType != null ? "updateType='" + updateType + '\'' : "") +
                (code != null ? "code='" + code + '\'' : "") +
                (name != null ? ", name='" + name + '\'' : "") +
                (structure != null ? ", structure='" + structure + '\'' : "") +
                (data != null ? ", data='" + data + '\'' : "") +
                (file != null ? ", file=" + file : "") +
                '}';
    }
}
