package ru.inovus.ms.rdm.api.model.compare;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

import static ru.inovus.ms.rdm.api.util.ComparableUtils.getStrongestStatus;

public class RdmComparable {

    private DiffStatusEnum status;

    RdmComparable() {
    }

    RdmComparable(DiffStatusEnum status) {
        this.status = status;
    }

    public DiffStatusEnum getStatus() {
        return status;
    }

    public void setStatus(DiffStatusEnum status) {
        this.status = this.status == null
                ? status
                : getStrongestStatus(this.status, status);
    }

}
