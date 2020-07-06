package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель изменения записей черновика из файла",
        description = "Набор входных параметров для изменения записей черновика из файла")
public class UpdateFromFileRequest implements DraftChangeRequest {

    @ApiModelProperty("Идентификатор версии-черновика")
    private Integer versionId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    @ApiModelProperty("Файл")
    private FileModel fileModel;

    public UpdateFromFileRequest() {
    }

    public UpdateFromFileRequest(Integer versionId, Integer optLockValue, FileModel fileModel) {
        this.versionId = versionId;
        this.optLockValue = optLockValue;
        this.fileModel = fileModel;
    }

    @Override
    public Integer getVersionId() {
        return versionId;
    }

    @Override
    public void setVersionId(Integer draftId) {
        this.versionId = draftId;
    }

    @Override
    public Integer getOptLockValue() {
        return optLockValue;
    }

    @Override
    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public FileModel getFileModel() {
        return fileModel;
    }

    public void setFileModel(FileModel fileModel) {
        this.fileModel = fileModel;
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
