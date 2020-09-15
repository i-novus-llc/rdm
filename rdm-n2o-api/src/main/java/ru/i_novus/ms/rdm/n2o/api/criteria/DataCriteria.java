package ru.i_novus.ms.rdm.n2o.api.criteria;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import org.apache.commons.lang3.BooleanUtils;
import ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil;

import java.io.Serializable;
import java.util.Map;

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
        if (sorting != null)
            super.setSorting(new Sorting(RdmUiUtil.deletePrefix(sorting.getField()), sorting.getDirection()));
    }

    public void setSorting(Map<String, String> sorting) {
        if (sorting == null) return;

        sorting.entrySet().stream().findFirst().ifPresent(e -> {
            Direction direction = e.getValue() == null || "ASC".equalsIgnoreCase(e.getValue()) ? Direction.ASC : Direction.DESC;
            setSorting(new Sorting(RdmUiUtil.deletePrefix(e.getKey()), direction));
        });
    }

    public boolean isHasDataConflict() {

        return BooleanUtils.isTrue(hasDataConflict) && (localeCode == null);
    }
}
