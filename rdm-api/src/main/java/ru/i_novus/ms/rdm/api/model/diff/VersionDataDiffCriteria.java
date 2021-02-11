package ru.i_novus.ms.rdm.api.model.diff;

import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Критерий поиска разницы между данными версий. */
@SuppressWarnings("unused")
public class VersionDataDiffCriteria extends AbstractCriteria {

    /** Идентификатор старой версии. */
    private Integer oldVersionId;

    /** Идентификатор новой версии. */
    private Integer newVersionId;

    /** Список первичных значений для исключения. */
    private List<String> excludePrimaryValues;

    /** Множество фильтров по первичным полям. */
    private Set<List<AttributeFilter>> primaryAttributesFilters;

    public VersionDataDiffCriteria() {
        // Nothing to do.
    }

    public VersionDataDiffCriteria(Integer oldVersionId, Integer newVersionId) {
        super();

        this.oldVersionId = oldVersionId;
        this.newVersionId = newVersionId;
    }

    public VersionDataDiffCriteria(VersionDataDiffCriteria criteria) {

        super(criteria);

        this.oldVersionId = criteria.oldVersionId;
        this.newVersionId = criteria.newVersionId;
        this.excludePrimaryValues = criteria.excludePrimaryValues;
        this.primaryAttributesFilters = criteria.primaryAttributesFilters;
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

    public List<String> getExcludePrimaryValues() {
        return excludePrimaryValues;
    }

    public void setExcludePrimaryValues(List<String> excludePrimaryValues) {
        this.excludePrimaryValues = excludePrimaryValues;
    }

    public Set<List<AttributeFilter>> getPrimaryAttributesFilters() {
        return primaryAttributesFilters;
    }

    public void setPrimaryAttributesFilters(Set<List<AttributeFilter>> primaryAttributesFilters) {
        this.primaryAttributesFilters = primaryAttributesFilters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionDataDiffCriteria)) return false;
        if (!super.equals(o)) return false;

        VersionDataDiffCriteria that = (VersionDataDiffCriteria) o;
        return Objects.equals(oldVersionId, that.oldVersionId) &&
                Objects.equals(newVersionId, that.newVersionId) &&
                Objects.equals(excludePrimaryValues, that.excludePrimaryValues) &&
                Objects.equals(primaryAttributesFilters, that.primaryAttributesFilters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), oldVersionId, newVersionId,
                excludePrimaryValues, primaryAttributesFilters);
    }
}
