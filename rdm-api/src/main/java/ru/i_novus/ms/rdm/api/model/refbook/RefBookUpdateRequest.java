package ru.i_novus.ms.rdm.api.model.refbook;

import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Objects;

public class RefBookUpdateRequest extends RefBookCreateRequest {

    /** Идентификатор версии справочника. */
    private Integer versionId;

    /** Значение оптимистической блокировки версии. */
    private Integer optLockValue;

    /** Комментарий к версии. */
    private String comment;

    public RefBookUpdateRequest() {
        // Nothing to do.
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RefBookUpdateRequest that = (RefBookUpdateRequest) o;
        return Objects.equals(versionId, that.versionId) &&
                Objects.equals(optLockValue, that.optLockValue) &&
                Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), versionId, optLockValue, comment);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
