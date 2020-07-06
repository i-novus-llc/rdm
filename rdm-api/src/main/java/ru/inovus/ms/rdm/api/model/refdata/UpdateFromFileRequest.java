package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель изменения записей черновика из файла",
        description = "Набор входных параметров для изменения записей черновика из файла")
public class UpdateFromFileRequest implements DraftChangeRequest {

    @ApiModelProperty("Идентификатор черновика")
    private Integer draftId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    @ApiModelProperty("Файл")
    private FileModel fileModel;

    public UpdateFromFileRequest() {
    }

    public UpdateFromFileRequest(Integer draftId, Integer optLockValue, FileModel fileModel) {
        this.draftId = draftId;
        this.optLockValue = optLockValue;
        this.fileModel = fileModel;
    }

    public Integer getDraftId() {
        return draftId;
    }

    public void setDraftId(Integer draftId) {
        this.draftId = draftId;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

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
