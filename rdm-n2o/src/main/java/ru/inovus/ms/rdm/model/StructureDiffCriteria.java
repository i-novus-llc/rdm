package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

/**
 * Created by znurgaliev on 20.09.2018.
 */
public class StructureDiffCriteria extends AbstractCriteria {

    private Integer oldVersionId;
    private Integer newVersionId;
    private DiffStatusEnum diffStatus;


    public StructureDiffCriteria() {
    }

    public StructureDiffCriteria(Integer oldVersionId, Integer newVersionId, DiffStatusEnum diffStatus) {
        this.oldVersionId = oldVersionId;
        this.newVersionId = newVersionId;
        this.diffStatus = diffStatus;
    }

    public Integer getOldVersionId() {
        return oldVersionId;
    }

    public void setOldVersionId(Integer oldVersionId) {
        this.oldVersionId = oldVersionId;
    }

    public Integer getNewVersionId() {
        return newVersionId;
    }

    public void setNewVersionId(Integer newVersionId) {
        this.newVersionId = newVersionId;
    }

    public DiffStatusEnum getDiffStatus() {
        return diffStatus;
    }

    public void setDiffStatus(DiffStatusEnum diffStatus) {
        this.diffStatus = diffStatus;
    }
}
