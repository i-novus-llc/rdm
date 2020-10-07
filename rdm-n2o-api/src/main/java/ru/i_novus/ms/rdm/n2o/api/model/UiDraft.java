package ru.i_novus.ms.rdm.n2o.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import java.util.Objects;

/**
 * Модель черновика для отображения.
 */
@ApiModel("Черновик для отображения")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UiDraft extends Draft {

    @ApiModelProperty("Идентификатор справочника")
    private Integer refBookId;

    public UiDraft(Integer id, Integer refBookId, Integer optLockValue) {

        super(id, null, optLockValue);

        this.refBookId = refBookId;
    }

    public UiDraft(RefBookVersion version) {
        this(version.getId(), version.getRefBookId(), version.getOptLockValue());
    }

    public UiDraft(Draft draft, Integer refBookId) {
        this(draft.getId(), refBookId, draft.getOptLockValue());
    }

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UiDraft)) return false;
        if (!super.equals(o)) return false;

        UiDraft that = (UiDraft) o;
        return Objects.equals(refBookId, that.refBookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), refBookId);
    }
}
