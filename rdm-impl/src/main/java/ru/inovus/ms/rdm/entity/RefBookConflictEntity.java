package ru.inovus.ms.rdm.entity;

import ru.inovus.ms.rdm.enumeration.ConflictType;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ref_book_conflict", schema = "n2o_rdm_management")
public class RefBookConflictEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "referrer_id", nullable = false)
    private RefBookVersionEntity referrerVersion;

    @ManyToOne
    @JoinColumn(name = "published_id", nullable = false)
    private RefBookVersionEntity publishedVersion;

    @Column(name = "ref_recordid", nullable = false)
    private Long refRecordId;

    @Column(name = "ref_field_code", nullable = false)
    private String refFieldCode;

    @Column(name = "conflict_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ConflictType conflictType;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    public RefBookConflictEntity() {
    }

    public RefBookConflictEntity(RefBookVersionEntity referrerVersion, RefBookVersionEntity publishedVersion,
                                 Long refRecordId, String refFieldCode, ConflictType conflictType) {
        this.referrerVersion = referrerVersion;
        this.publishedVersion = publishedVersion;
        this.refRecordId = refRecordId;
        this.refFieldCode = refFieldCode;
        this.conflictType = conflictType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Long getRefRecordId() {
        return refRecordId;
    }

    public void setRefRecordId(Long refRecordId) {
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

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (creationDate == null)
            creationDate = now;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookConflictEntity that = (RefBookConflictEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(referrerVersion, that.referrerVersion) &&
                Objects.equals(publishedVersion, that.publishedVersion) &&
                Objects.equals(refRecordId, that.refRecordId) &&
                Objects.equals(refFieldCode, that.refFieldCode) &&
                Objects.equals(conflictType, that.conflictType) &&
                Objects.equals(creationDate, that.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, referrerVersion, publishedVersion, refRecordId,
                refFieldCode, conflictType, creationDate);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RefBookConflictEntity{");
        sb.append("id=").append(id);
        sb.append(", referrerVersion=").append(referrerVersion);
        sb.append(", publishedVersion=").append(publishedVersion);
        sb.append(", refRecordId=").append(refRecordId);
        sb.append(", refFieldCode='").append(refFieldCode).append('\'');
        sb.append(", conflictType=").append(conflictType);
        sb.append(", creationDate=").append(creationDate);
        sb.append('}');
        return sb.toString();
    }
}
