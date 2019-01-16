package ru.inovus.ms.rdm.model;

public class UiDraft extends Draft {
    private Integer refBookId;

    public UiDraft() {
    }

    public UiDraft(Integer id, Integer refBookId) {
        super(id, null);
        this.refBookId = refBookId;
    }

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }
}
