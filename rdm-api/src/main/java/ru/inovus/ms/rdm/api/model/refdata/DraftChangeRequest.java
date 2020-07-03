package ru.inovus.ms.rdm.api.model.refdata;

import java.io.Serializable;

public interface DraftChangeRequest extends Serializable {

    Integer getDraftId();
    void setDraftId(Integer draftId);

    Integer getOptLockValue();
    void setOptLockValue(Integer optLockValue);
}
