package ru.inovus.ms.rdm.n2o.model.refbook;

public class RefBookUpdateRequest extends RefBookCreateRequest {

    private Integer versionId;
    private String comment;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
