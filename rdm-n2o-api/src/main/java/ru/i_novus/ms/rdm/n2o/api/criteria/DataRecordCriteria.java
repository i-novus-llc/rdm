package ru.i_novus.ms.rdm.n2o.api.criteria;

import io.swagger.annotations.ApiParam;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import javax.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Критерий поиска записи справочника.
 */
@SuppressWarnings("unused") // used in: DataRecordQueryProvider
public class DataRecordCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор записи")
    @QueryParam("id")
    private Integer id;

    @ApiParam("Идентификатор версии")
    @QueryParam("versionId")
    private Integer versionId;

    @ApiParam("Идентификатор записи")
    @QueryParam("optLockValue")
    private Integer optLockValue;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
        return Objects.equals(id, that.id) &&
                Objects.equals(versionId, that.versionId) &&
                Objects.equals(optLockValue, that.optLockValue) &&
                Objects.equals(localeCode, that.localeCode) &&
                Objects.equals(dataAction, that.dataAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, versionId, optLockValue, localeCode, dataAction);
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
