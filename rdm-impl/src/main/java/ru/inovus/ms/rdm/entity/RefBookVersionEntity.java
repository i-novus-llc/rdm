package ru.inovus.ms.rdm.entity;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.RefBookCreateRequest;
import ru.inovus.ms.rdm.model.RefBookUpdateRequest;
import ru.inovus.ms.rdm.model.Structure;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ref_book_version", schema = "n2o_rdm_management")
@TypeDef(name = "structure", typeClass = StructureType.class)
public class RefBookVersionEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "ref_book_id", nullable = false)
    private RefBookEntity refBook;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "short_name", nullable = false)
    private String shortName;

    @Column(name = "annotation")
    private String annotation;

    @Column(name = "structure", columnDefinition = "json")
    @Type(type = "structure")
    private Structure structure;

    @Column(name = "storage_code")
    private String storageCode;

    @Column(name = "version")
    private String version;

    @Column(name = "comment")
    private String comment;

    @Column(name = "status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private RefBookVersionStatus status;

    @Column(name = "from_date")
    private LocalDateTime fromDate;

    @Column(name = "to_date")
    private LocalDateTime toDate;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "last_action_date")
    private LocalDateTime lastActionDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RefBookEntity getRefBook() {
        return refBook;
    }

    public void setRefBook(RefBookEntity refBook) {
        this.refBook = refBook;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public RefBookVersionStatus getStatus() {
        return status;
    }

    public void setStatus(RefBookVersionStatus status) {
        this.status = status;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(LocalDateTime lastActionDate) {
        this.lastActionDate = lastActionDate;
    }

    public String getStorageCode() {
        return storageCode;
    }

    public void setStorageCode(String storageCode) {
        this.storageCode = storageCode;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (creationDate == null)
            creationDate = now;

        if (lastActionDate == null)
            lastActionDate = now;
    }

    @SuppressWarnings("all")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookVersionEntity that = (RefBookVersionEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (refBook != null ? !refBook.equals(that.refBook) : that.refBook != null) return false;
        if (fullName != null ? !fullName.equals(that.fullName) : that.fullName != null) return false;
        if (shortName != null ? !shortName.equals(that.shortName) : that.shortName != null) return false;
        if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null) return false;
        if (structure != null ? !structure.equals(that.structure) : that.structure != null) return false;
        if (storageCode != null ? !storageCode.equals(that.storageCode) : that.storageCode != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
        if (status != that.status) return false;
        if (fromDate != null ? !fromDate.equals(that.fromDate) : that.fromDate != null) return false;
        if (toDate != null ? !toDate.equals(that.toDate) : that.toDate != null) return false;
        if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null) return false;
        return lastActionDate != null ? lastActionDate.equals(that.lastActionDate) : that.lastActionDate == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (refBook != null ? refBook.hashCode() : 0);
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        result = 31 * result + (shortName != null ? shortName.hashCode() : 0);
        result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
        result = 31 * result + (structure != null ? structure.hashCode() : 0);
        result = 31 * result + (storageCode != null ? storageCode.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (fromDate != null ? fromDate.hashCode() : 0);
        result = 31 * result + (toDate != null ? toDate.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (lastActionDate != null ? lastActionDate.hashCode() : 0);
        return result;
    }

    @PreUpdate
    public void preUpdate() {
        lastActionDate = LocalDateTime.now();
    }

    public void populateFrom(RefBookCreateRequest model) {
        this.setFullName(model.getFullName());
        this.setShortName(model.getShortName());
        this.setAnnotation(model.getAnnotation());
    }

    public void populateFrom(RefBookUpdateRequest model) {
        populateFrom((RefBookCreateRequest) model);
        this.setComment(model.getComment());
    }
}