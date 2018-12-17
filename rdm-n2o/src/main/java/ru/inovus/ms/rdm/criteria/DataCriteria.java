package ru.inovus.ms.rdm.criteria;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by znurgaliev on 14.11.2018.
 */
public class DataCriteria extends Criteria {
    private Integer versionId;
    private Map<String, Serializable> filter;

    public DataCriteria() {
    }

    public DataCriteria(Integer versionId, Map<String, Serializable> filter) {
        this.versionId = versionId;
        this.filter = filter;
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

    public void setSorting(Map<String, String> sorting) {
        if (sorting == null) return;
        sorting.entrySet().stream().findFirst().ifPresent(e -> {
            Direction direction = e.getValue() == null || "ASC".equalsIgnoreCase(e.getValue()) ? Direction.ASC : Direction.DESC;
            setSorting(new Sorting(e.getKey(), direction));
        });

    }
}
