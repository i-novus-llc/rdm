package ru.inovus.ms.rdm.impl.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Данные проверки существования конфликтов.
 *
 * Используется для заполнения RefBook.
 */
@Entity
@SuppressWarnings("unused")
public class ExistsConflictData {

    @Id
    @Column(name = "referrer_version_id")
    private Integer referrerVersionId;

    /** Наличие конфликта данных */
    @Column(name = "has_data_conflict")
    private Boolean hasDataConflict;

    /** Наличие конфликта обновления записи */
    @Column(name = "has_updated_conflict")
    private Boolean hasUpdatedConflict;

    /** Наличие конфликта изменения структуры */
    @Column(name = "has_altered_conflict")
    private Boolean hasAlteredConflict;

    /** Наличие конфликта структуры */
    @Column(name = "has_structure_conflict")
    private Boolean hasStructureConflict;

    /** Наличие конфликта в последней опубликованной версии */
    @Column(name = "last_has_data_conflict")
    private Boolean lastHasDataConflict;

    public ExistsConflictData() {
    }

    public Integer getReferrerVersionId() {
        return referrerVersionId;
    }

    public void setReferrerVersionId(Integer referrerVersionId) {
        this.referrerVersionId = referrerVersionId;
    }

    public Boolean getHasDataConflict() {
        return hasDataConflict;
    }

    public void setHasDataConflict(Boolean hasDataConflict) {
        this.hasDataConflict = hasDataConflict;
    }

    public Boolean getHasUpdatedConflict() {
        return hasUpdatedConflict;
    }

    public void setHasUpdatedConflict(Boolean hasUpdatedConflict) {
        this.hasUpdatedConflict = hasUpdatedConflict;
    }

    public Boolean getHasAlteredConflict() {
        return hasAlteredConflict;
    }

    public void setHasAlteredConflict(Boolean hasAlteredConflict) {
        this.hasAlteredConflict = hasAlteredConflict;
    }

    public Boolean getHasStructureConflict() {
        return hasStructureConflict;
    }

    public void setHasStructureConflict(Boolean hasStructureConflict) {
        this.hasStructureConflict = hasStructureConflict;
    }

    public Boolean getLastHasDataConflict() {
        return lastHasDataConflict;
    }

    public void setLastHasDataConflict(Boolean lastHasDataConflict) {
        this.lastHasDataConflict = lastHasDataConflict;
    }
}
