package ru.i_novus.ms.rdm.n2o.api.criteria;

import io.swagger.annotations.ApiParam;
import jakarta.ws.rs.QueryParam;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Objects;

/**
 * Критерий поиска записи справочника.
 */
@SuppressWarnings("unused") // used in: DataRecordQueryProvider
public class DataRecordCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор версии")
    @QueryParam("versionId")
    private Integer versionId;

    @ApiParam("Значение оптимистической блокировки версии")
    @QueryParam("optLockValue")
    private Integer optLockValue;

    @ApiParam("Идентификатор записи")
    @QueryParam("id")
    private Long id;

    @ApiParam("Код локали")
    @QueryParam("localeCode")
    private String localeCode;

    @ApiParam("Действие над записью")
    @QueryParam("dataAction")
    private String dataAction;

    public DataRecordCriteria() {
        // Nothing to do.
    }

    public DataRecordCriteria(int pageNumber, int pageSize) {
        super(pageNumber, pageSize);
    }

    public DataRecordCriteria(int pageNumber, int pageSize, Sort sort) {
        super(pageNumber, pageSize, sort);
    }

    public DataRecordCriteria(DataRecordCriteria criteria) {

        super(criteria);

        this.versionId = criteria.getVersionId();
        this.optLockValue = criteria.getOptLockValue();

        this.id = criteria.getId();
        this.localeCode = criteria.getLocaleCode();
        this.dataAction = criteria.getDataAction();
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getDataAction() {
        return dataAction;
    }

    public void setDataAction(String dataAction) {
        this.dataAction = dataAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DataRecordCriteria that = (DataRecordCriteria) o;
        return Objects.equals(versionId, that.versionId) &&
                Objects.equals(optLockValue, that.optLockValue) &&
                
                Objects.equals(id, that.id) &&
                Objects.equals(localeCode, that.localeCode) &&
                Objects.equals(dataAction, that.dataAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), versionId, optLockValue, id, localeCode, dataAction);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + JsonUtil.toJsonString(this);
    }
}
