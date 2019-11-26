package ru.inovus.ms.rdm.n2o.criteria;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import ru.inovus.ms.rdm.n2o.util.RdmUiUtil;

import java.io.Serializable;
import java.util.Map;

import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.deletePrefix;

/**
 * Критерий поиска записей версии справочника.
 *
 * Created by znurgaliev on 14.11.2018.
 */
@SuppressWarnings("unused")
public class DataCriteria extends Criteria {

    private Integer versionId;
    private Map<String, Serializable> filter;
    private Boolean hasDataConflict;

    public DataCriteria() {
    }

    public DataCriteria(Integer versionId, Map<String, Serializable> filter, Boolean hasDataConflict) {
        this.versionId = versionId;
        this.filter = filter;
        this.hasDataConflict = hasDataConflict;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
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
}
