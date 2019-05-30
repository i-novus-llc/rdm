package ru.inovus.ms.rdm.entity;

import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.util.TimeUtils;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ref_book_conflict", schema = "n2o_rdm_management")
public class RefBookConflictEntity {

    @ManyToOne
    @JoinColumn(name = "referrer_id", nullable = false)
    private RefBookVersionEntity referrerVersion;

    @ManyToOne
    @JoinColumn(name = "published_id", nullable = false)
    private RefBookVersionEntity publishedVersion;

    @Column(name = "ref_recordid", nullable = false)
    private Integer refRecordId;

    @Column(name = "ref_field_code", nullable = false)
    private String refFieldCode;

    @Column(name = "conflict_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ConflictType conflictType;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "closure_date")
    private LocalDateTime closureDate;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = TimeUtils.now();

        if (creationDate == null)
            creationDate = now;
    }

    @SuppressWarnings("all")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookConflictEntity that = (RefBookConflictEntity) o;
        return Objects.equals(referrerVersion, that.referrerVersion) &&
                Objects.equals(publishedVersion, that.publishedVersion) &&
                Objects.equals(refRecordId, that.refRecordId) &&
                Objects.equals(refFieldCode, that.refFieldCode) &&
                Objects.equals(conflictType, that.conflictType) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(closureDate, that.closureDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referrerVersion, publishedVersion, refRecordId,
                refFieldCode, conflictType, creationDate, closureDate);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RefBookConflictEntity{");
        sb.append("referrerVersion=").append(referrerVersion);
        sb.append(", publishedVersion=").append(publishedVersion);
        sb.append(", refRecordId=").append(refRecordId);
        sb.append(", refFieldCode='").append(refFieldCode).append('\'');
        sb.append(", conflictType=").append(conflictType);
        sb.append(", creationDate=").append(creationDate);
        sb.append(", closureDate=").append(closureDate);
        sb.append('}');
        return sb.toString();
    }

    public RefBookVersionEntity getReferrerVersion() {
        return referrerVersion;
    }

    public void setReferrerVersion(RefBookVersionEntity referrerVersion) {
        this.referrerVersion = referrerVersion;
    }

    public RefBookVersionEntity getPublishedVersion() {
        return publishedVersion;
    }

    public void setPublishedVersion(RefBookVersionEntity publishedVersion) {
        this.publishedVersion = publishedVersion;
    }

    public Integer getRefRecordId() {
        return refRecordId;
    }

    public void setRefRecordId(Integer refRecordId) {
        this.refRecordId = refRecordId;
    }

    public String getRefFieldCode() {
        return refFieldCode;
    }

    public void setRefFieldCode(String refFieldCode) {
        this.refFieldCode = refFieldCode;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public void setConflictType(ConflictType conflictType) {
        this.conflictType = conflictType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getClosureDate() {
        return closureDate;
    }

    public void setClosureDate(LocalDateTime closureDate) {
        this.closureDate = closureDate;
    }
}
