package ru.i_novus.ms.rdm.rest.loader;

import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Objects;

import static ru.i_novus.ms.rdm.rest.loader.RefBookDataUpdateTypeEnum.CREATE_ONLY;

/** Запрос на загрузку справочников через RefBookDataServerLoaderRunner. */
public class RefBookDataRequest extends RefBookCreateRequest {

    /** Идентификатор изменения справочника. */
    private String changeSetId;

    /** Тип изменения справочника. */
    private RefBookDataUpdateTypeEnum updateType = CREATE_ONLY;

    /** Структура справочника. */
    private String structure;

    /** Данные справочника. */
    private String data;

    /** Модель файла справочника. */
    private FileModel fileModel;

    public RefBookDataRequest() {
        // Nothing to do.
    }

    public String getChangeSetId() {
        return changeSetId;
    }

    public void setChangesetId(String changeSetId) {
        this.changeSetId = changeSetId;
    }

    public RefBookDataUpdateTypeEnum getUpdateType() {
        return updateType;
    }

    public void setUpdateType(RefBookDataUpdateTypeEnum updateType) {
        this.updateType = updateType;
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
    @SuppressWarnings("java:S2159")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final RefBookDataRequest that = (RefBookDataRequest) o;
        return Objects.equals(changeSetId, that.changeSetId) &&
                (updateType == that.updateType) &&
                Objects.equals(structure, that.structure) &&
                Objects.equals(data, that.data) &&
                Objects.equals(fileModel, that.fileModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), changeSetId, updateType, structure, data);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + JsonUtil.toJsonString(this);
    }
}
