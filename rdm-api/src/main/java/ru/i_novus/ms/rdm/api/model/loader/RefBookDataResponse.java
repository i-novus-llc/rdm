package ru.i_novus.ms.rdm.api.model.loader;

import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Загрузка справочника: Ответ.
 */
public class RefBookDataResponse implements Serializable {

    /** Идентификатор загруженного справочника. */
    private Integer refBookId;

    /** Дата загрузки справочника. */
    private LocalDateTime executedDate;

    public RefBookDataResponse() {
        // Nothing to do.
    }

    public RefBookDataResponse(Integer refBookId, LocalDateTime executedDate) {

        this.refBookId = refBookId;
        this.executedDate = executedDate;
    }

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

    public LocalDateTime getExecutedDate() {
        return executedDate;
    }

    public void setExecutedDate(LocalDateTime executedDate) {
        this.executedDate = executedDate;
    }

    @Override
    @SuppressWarnings("java:S2159")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        final RefBookDataResponse that = (RefBookDataResponse) o;
        return Objects.equals(refBookId, that.refBookId) &&
                Objects.equals(executedDate, that.executedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refBookId, executedDate);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + JsonUtil.toJsonString(this);
    }
}
