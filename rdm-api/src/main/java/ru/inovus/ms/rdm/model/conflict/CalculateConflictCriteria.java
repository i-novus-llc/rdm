package ru.inovus.ms.rdm.model.conflict;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.compare.CompareCriteria;

import javax.ws.rs.QueryParam;

import java.util.Objects;

import static ru.inovus.ms.rdm.util.ConflictUtils.*;

@ApiModel("Критерии вычисления конфликтов")
public class CalculateConflictCriteria extends CompareCriteria {

    @ApiModelProperty("Идентификатор версии справочника со ссылками")
    @QueryParam("referrerVersionId")
    private Integer referrerVersionId;

    @ApiModelProperty("Максимальное количество вычисляемых конфликтов")
    @QueryParam("maxResultLimit")
    private Integer maxResultLimit;

    //@ApiModelProperty("Тип конфликта")

    @SuppressWarnings("unused")
    public CalculateConflictCriteria() {
    }

    public CalculateConflictCriteria(Integer referrerVersionId, Integer oldVersionId, Integer newVersionId) {
        super(oldVersionId, newVersionId, null);
        this.referrerVersionId = referrerVersionId;
    }

    public CalculateConflictCriteria(CompareCriteria criteria) {
        super(criteria.getOldVersionId(), criteria.getNewVersionId(), criteria.getDiffStatus());
    }

    public Integer getReferrerVersionId() {
        return referrerVersionId;
    }

    public void setReferrerVersionId(Integer referrerVersionId) {
        this.referrerVersionId = referrerVersionId;
    }

    public Integer getMaxResultLimit() {
        return maxResultLimit;
    }

    public void setMaxResultLimit(Integer maxResultLimit) {
        this.maxResultLimit = maxResultLimit;
    }

    public ConflictType getConflictType() {
        return diffStatusToConflictType(getDiffStatus());
    }

    public void setConflictType(ConflictType conflictType) {
        setDiffStatus(conflictTypeToDiffStatus(conflictType));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CalculateConflictCriteria that = (CalculateConflictCriteria) o;
        return Objects.equals(referrerVersionId, that.referrerVersionId) &&
                Objects.equals(maxResultLimit, that.maxResultLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), referrerVersionId, maxResultLimit);
    }
}
