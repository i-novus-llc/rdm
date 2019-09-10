package ru.inovus.ms.rdm.model;

import ru.inovus.ms.rdm.model.draft.Draft;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UiDraft)) return false;
        if (!super.equals(o)) return false;
        UiDraft uiDraft = (UiDraft) o;
        return Objects.equals(refBookId, uiDraft.refBookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), refBookId);
    }
}
