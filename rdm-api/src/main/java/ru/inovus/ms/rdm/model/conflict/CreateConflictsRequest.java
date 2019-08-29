package ru.inovus.ms.rdm.model.conflict;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@ApiModel(value = "Модель создания конфликтов", description = "Набор данных для создания конфликтов")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateConflictsRequest implements Serializable {

    @ApiModelProperty("Идентификатор версии, которая ссылается")
    @QueryParam("refFromId")
    private Integer refFromId;

    @ApiModelProperty("Идентификатор версии, на которую ссылаются")
    @QueryParam("refToId")
    private Integer refToId;

    @ApiModelProperty("Список конфликтов")
    @QueryParam("conflicts")
    private List<Conflict> conflicts;

    @SuppressWarnings("unused")
    public CreateConflictsRequest() {

    }

    public CreateConflictsRequest(Integer refFromId, Integer refToId, List<Conflict> conflicts) {
        this.refFromId = refFromId;
        this.refToId = refToId;
        this.conflicts = conflicts;
    }

    public Integer getRefFromId() {
        return refFromId;
    }

    public void setRefFromId(Integer refFromId) {
        this.refFromId = refFromId;
    }

    public Integer getRefToId() {
        return refToId;
    }

    public void setRefToId(Integer refToId) {
        this.refToId = refToId;
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<Conflict> conflicts) {
        this.conflicts = conflicts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateConflictsRequest that = (CreateConflictsRequest) o;
        return Objects.equals(refFromId, that.refFromId) &&
                Objects.equals(refToId, that.refToId) &&
                Objects.equals(conflicts, that.conflicts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refFromId, refToId, conflicts);
    }
}
