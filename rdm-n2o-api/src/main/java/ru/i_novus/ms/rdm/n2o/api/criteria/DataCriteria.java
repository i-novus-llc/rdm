package ru.i_novus.ms.rdm.n2o.api.criteria;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import org.apache.commons.lang3.BooleanUtils;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import static ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil.deletePrefix;

/**
 * Критерий поиска записей версии справочника.
 *
 * Created by znurgaliev on 14.11.2018.
 */
@SuppressWarnings("unused")
public class DataCriteria extends Criteria {

    /**
     * Идентификатор версии справочника.
     */
    private Integer versionId;

    /**
     * Значение оптимистической блокировки версии.
     */
    private Integer optLockValue;

    /**
     * Код локали.
     */
    private String localeCode;

    /**
     * Фильтр по атрибутам: код = значение.
     */
    private Map<String, Serializable> filter;

    /**
     * Наличие конфликта данных.
     */
    private Boolean hasDataConflict;

    public DataCriteria() {
        // Nothing to do.
    }

    public DataCriteria(Integer versionId, Integer optLockValue,
                        Map<String, Serializable> filter, Boolean hasDataConflict) {

        this.versionId = versionId;
        this.optLockValue = optLockValue;

        this.filter = filter;
        this.hasDataConflict = hasDataConflict;
    }

    public DataCriteria(DataCriteria criteria) {

        super(criteria);

        this.versionId = criteria.getVersionId();
        this.optLockValue = criteria.getOptLockValue();
        this.localeCode = criteria.getLocaleCode();

        this.filter = criteria.getFilter();
        this.hasDataConflict = criteria.getHasDataConflict();
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public Map<String, Serializable> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Serializable> filter) {
        this.filter = filter;
    }

    public Boolean getHasDataConflict() {
        return hasDataConflict;
    }

    public void setHasDataConflict(Boolean hasDataConflict) {
        this.hasDataConflict = hasDataConflict;
    }

    @Override
    public void setSorting(Sorting sorting) {

        if (sorting == null) return;

        super.setSorting(toSorting(sorting.getField(), sorting.getDirection()));
    }

    public void setSorting(Map<String, String> sorting) {

        if (sorting == null) return;

        sorting.entrySet().stream().findFirst().ifPresent(e ->
                setSorting(toSorting(e.getKey(), toDirection(e.getValue())))
        );
    }

    private Sorting toSorting(String fieldName, Direction direction) {

        return new Sorting(deletePrefix(fieldName), direction);
    }

    private Direction toDirection(String value) {

        return value == null || "ASC".equalsIgnoreCase(value) ? Direction.ASC : Direction.DESC;
    }

    public boolean isHasDataConflict() {

        return BooleanUtils.isTrue(hasDataConflict) && (localeCode == null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if(!superEquals(o)) return false;

        DataCriteria that = (DataCriteria) o;
        return Objects.equals(versionId, that.versionId) &&
                Objects.equals(optLockValue, that.optLockValue) &&
                Objects.equals(localeCode, that.localeCode) &&
                Objects.equals(filter, that.filter) &&
                Objects.equals(hasDataConflict, that.hasDataConflict);
    }

    private boolean superEquals(Object o) {
        if (!(o instanceof Criteria)) return false;

        Criteria that = (Criteria) o;
        return Objects.equals(getPage(), that.getPage()) &&
                Objects.equals(getSize(), that.getSize()) &&
                Objects.equals(getCount(), that.getCount());
    }

    @Override
    public int hashCode() {
        return Objects.hash(superHashCode(), versionId, optLockValue, localeCode, filter, hasDataConflict);
    }

    private int superHashCode() {
        return Objects.hash(getPage(), getSize(), getCount());
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
