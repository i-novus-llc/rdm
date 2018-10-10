package ru.inovus.ms.rdm.model.compare;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

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
        this.status = status;
    }

}
