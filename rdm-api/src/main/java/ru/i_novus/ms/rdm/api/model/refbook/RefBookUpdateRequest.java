package ru.i_novus.ms.rdm.api.model.refbook;

public class RefBookUpdateRequest extends RefBookCreateRequest {

    /** Идентификатор версии справочника. */
    private Integer versionId;

    /** Значение оптимистической блокировки версии. */
    private Integer optLockValue;

    /** Комментарий к версии. */
    private String comment;

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
}
