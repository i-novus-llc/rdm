package ru.i_novus.ms.rdm.rest.loader;

import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Objects;

/** Запрос на загрузку справочников через RefBookDataServerLoaderRunner. */
public class RefBookDataRequest extends RefBookCreateRequest {

    /** Структура справочника. */
    private String structure;

    /** Данные справочника. */
    private String data;

    /** Модель файла справочника. */
    private FileModel fileModel;

    public RefBookDataRequest() {
        // Nothing to do.
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

    public FileModel getFileModel() {
        return fileModel;
    }

    public void setFileModel(FileModel fileModel) {
        this.fileModel = fileModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefBookDataRequest)) return false;
        if (!super.equals(o)) return false;

        RefBookDataRequest that = (RefBookDataRequest) o;
        return Objects.equals(structure, that.structure) &&
                Objects.equals(data, that.data) &&
                Objects.equals(fileModel, that.fileModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), structure, data);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
