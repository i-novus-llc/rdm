package ru.inovus.ms.rdm.entity;

import ru.inovus.ms.rdm.model.Metadata;
import ru.inovus.ms.rdm.model.RefBookVersionStatus;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "ref_book_version", schema = "n2o_rdm_management")
public class RefBookVersionEntity extends AbstractEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ref_book_id", nullable = false)
    private RefBookEntity refBook;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "short_name", nullable = false)
    private String shortName;

    @Column(name = "annotation")
    private String annotation;

    @Column(name = "structure")
    @Transient//todo
    private Metadata structure;

    @Column(name = "storage_code")
    private String storageCode;

    @Column(name = "version")
    private String version;

    @Column(name = "comment")
    private String comment;

    @Column(name = "status", nullable = false)
    @Enumerated
    private RefBookVersionStatus status;

    @Column(name = "from_date")
    private Date fromDate;

    @Column(name = "to_date")
    private Date toDate;

    @Column(name = "creation_date")
    private Date creationDate;

    @Column(name = "last_action_date")
    private Date lastActionDate;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
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

    public Metadata getStructure() {
        return structure;
    }

    public void setStructure(Metadata structure) {
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

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public Date getLastActionDate() {
        return lastActionDate;
    }

    @Override
    public void setLastActionDate(Date lastActionDate) {
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
        Date now = new Date();

        if (creationDate == null)
            creationDate = now;

        if (lastActionDate == null)
            lastActionDate = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefBookVersionEntity that = (RefBookVersionEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(refBook, that.refBook) &&
                Objects.equals(fullName, that.fullName) &&
                Objects.equals(shortName, that.shortName) &&
                Objects.equals(annotation, that.annotation) &&
                Objects.equals(structure, that.structure) &&
                Objects.equals(storageCode, that.storageCode) &&
                Objects.equals(version, that.version) &&
                Objects.equals(comment, that.comment) &&
                Objects.equals(status, that.status) &&
                Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(lastActionDate, that.lastActionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, refBook, fullName, shortName, annotation, structure, storageCode, version, comment, status, fromDate, toDate, creationDate, lastActionDate);
    }
}