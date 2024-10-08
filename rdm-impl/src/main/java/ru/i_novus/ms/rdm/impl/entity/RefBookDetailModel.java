package ru.i_novus.ms.rdm.impl.entity;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * Подробная информация о версии для заполнения модели RefBook.
 */
@Entity
@SuppressWarnings("unused")
public class RefBookDetailModel {

    /** Текущая (рассматриваемая) версия. */
    @Id
    @Column(name = "current_version_id")
    private Integer currentVersionId;

    /** Версия-черновик. */
    @ManyToOne
    @JoinColumn(name = "draft_version_id")
    private RefBookVersionEntity draftVersion;

    /** Последняя опубликованная версия. */
    @ManyToOne
    @JoinColumn(name = "last_published_version_id")
    private RefBookVersionEntity lastPublishedVersion;

    /** Удаляемость справочника. */
    @Column(name = "removable")
    private Boolean removable;

    /** Наличие связанного справочника (т.е. ссылающегося на данный). */
    @Column(name = "has_referrer_version")
    private Boolean hasReferrer;

    /** Наличие конфликта данных. */
    @Column(name = "has_data_conflict")
    private Boolean hasDataConflict;

    /** Наличие конфликта обновления записи. */
    @Column(name = "has_updated_conflict")
    private Boolean hasUpdatedConflict;

    /** Наличие конфликта изменения структуры. */
    @Column(name = "has_altered_conflict")
    private Boolean hasAlteredConflict;

    /** Наличие конфликта структуры. */
    @Column(name = "has_structure_conflict")
    private Boolean hasStructureConflict;

    /** Наличие конфликта в последней опубликованной версии. */
    @Column(name = "last_has_conflict")
    private Boolean lastHasConflict;

    public RefBookDetailModel() {
        // Nothing to do.
    }

    public Integer getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(Integer currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public RefBookVersionEntity getDraftVersion() {
        return draftVersion;
    }

    public void setDraftVersion(RefBookVersionEntity draftVersion) {
        this.draftVersion = draftVersion;
    }

    public RefBookVersionEntity getLastPublishedVersion() {
        return lastPublishedVersion;
    }

    public void setLastPublishedVersion(RefBookVersionEntity lastPublishedVersion) {
        this.lastPublishedVersion = lastPublishedVersion;
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }

    public Boolean getHasReferrer() {
        return hasReferrer;
    }

    public void setHasReferrer(Boolean hasReferrer) {
        this.hasReferrer = hasReferrer;
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

    public Boolean getLastHasConflict() {
        return lastHasConflict;
    }

    public void setLastHasConflict(Boolean lastHasConflict) {
        this.lastHasConflict = lastHasConflict;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookDetailModel that = (RefBookDetailModel) o;
        return Objects.equals(currentVersionId, that.currentVersionId) &&
                Objects.equals(draftVersion, that.draftVersion) &&
                Objects.equals(lastPublishedVersion, that.lastPublishedVersion) &&
                Objects.equals(removable, that.removable) &&
                Objects.equals(hasReferrer, that.hasReferrer) &&
                Objects.equals(hasDataConflict, that.hasDataConflict) &&
                Objects.equals(hasUpdatedConflict, that.hasUpdatedConflict) &&
                Objects.equals(hasAlteredConflict, that.hasAlteredConflict) &&
                Objects.equals(hasStructureConflict, that.hasStructureConflict) &&
                Objects.equals(lastHasConflict, that.lastHasConflict);
    }

    @Override
    public int hashCode() {

        return Objects.hash(currentVersionId, draftVersion, lastPublishedVersion, removable, hasReferrer,
                hasDataConflict, hasUpdatedConflict, hasAlteredConflict, hasStructureConflict, lastHasConflict);
    }
}
