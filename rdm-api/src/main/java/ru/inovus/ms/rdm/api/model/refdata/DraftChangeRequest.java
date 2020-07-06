package ru.inovus.ms.rdm.api.model.refdata;

import java.io.Serializable;

public interface DraftChangeRequest extends Serializable {

    Integer getOptLockValue();
    void setOptLockValue(Integer optLockValue);
}
