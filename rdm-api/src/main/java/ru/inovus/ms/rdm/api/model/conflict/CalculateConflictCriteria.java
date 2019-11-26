package ru.inovus.ms.rdm.api.model.conflict;

import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.model.compare.CompareCriteria;

import javax.ws.rs.QueryParam;

import java.util.Objects;

import static ru.inovus.ms.rdm.api.util.ConflictUtils.*;

/**
 * Критерий вычисления конфликтов.
 */
public class CalculateConflictCriteria extends CompareCriteria {

    @ApiParam("Идентификатор версии справочника со ссылками")
    @QueryParam("referrerVersionId")
    private Integer referrerVersionId;

    @ApiParam("Наличие изменения структуры")
    @QueryParam("structureAltered")
    private boolean structureAltered;

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

    public boolean getStructureAltered() {
        return structureAltered;
    }

    public void setStructureAltered(boolean structureAltered) {
        this.structureAltered = structureAltered;
    }

    @ApiParam("Тип конфликта")
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
        return Objects.equals(referrerVersionId, that.referrerVersionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), referrerVersionId);
    }
}
