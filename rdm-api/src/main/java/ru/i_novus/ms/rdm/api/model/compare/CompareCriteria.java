package ru.i_novus.ms.rdm.api.model.compare;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Created by znurgaliev on 20.09.2018.
 */

@ApiModel("Критерии сравнения данных версий справочника")
public class CompareCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор старой версии")
    @QueryParam("oldVersionId")
    private Integer oldVersionId;

    @ApiModelProperty("Идентификатор новой версии")
    @QueryParam("newVersionId")
    private Integer newVersionId;

    @ApiModelProperty("Статус типа возвращаемых данных")
    @QueryParam("diffStatus")
    private DiffStatusEnum diffStatus;

    @SuppressWarnings("WeakerAccess")
    public CompareCriteria() {
    }

    public CompareCriteria(Integer oldVersionId, Integer newVersionId, DiffStatusEnum diffStatus) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompareCriteria that = (CompareCriteria) o;
        return Objects.equals(oldVersionId, that.oldVersionId) &&
                Objects.equals(newVersionId, that.newVersionId) &&
                diffStatus == that.diffStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldVersionId, newVersionId, diffStatus);
    }

}
