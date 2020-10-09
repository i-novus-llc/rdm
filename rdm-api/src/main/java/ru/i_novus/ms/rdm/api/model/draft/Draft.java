package ru.i_novus.ms.rdm.api.model.draft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Objects;

/**
 * Модель черновика.
 */
@ApiModel("Черновик")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Draft {

    @ApiModelProperty("Идентификатор черновика")
    private Integer id;

    @ApiModelProperty("Код хранилища")
    private String storageCode;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    public Draft(Integer id, String storageCode, Integer optLockValue) {
        this.id = id;
        this.storageCode = storageCode;
        this.optLockValue = optLockValue;
    }

    public Draft() {
        // Nothing to do.
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStorageCode() {
        return storageCode;
    }

    public void setStorageCode(String storageCode) {
        this.storageCode = storageCode;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public boolean isVersionDraft(Integer versionId) {
        return Objects.equals(getId(), versionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Draft that = (Draft) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(storageCode, that.storageCode) &&
                Objects.equals(optLockValue, that.optLockValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storageCode, optLockValue);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
