package ru.inovus.ms.rdm.impl.entity;

import ru.inovus.ms.rdm.api.enumeration.RefBookOperation;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "ref_book_operation", schema = "n2o_rdm_management")
public class RefBookOperationEntity implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "ref_book_id", nullable = false)
    private RefBookEntity refBook;

    @Column(name = "operation")
    @Enumerated(value = EnumType.STRING)
    private RefBookOperation operation;

    @Column(name = "lock_id")
    private String lockId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    public RefBookOperationEntity(RefBookEntity refBook, RefBookOperation operation, String lockId, String userName) {
        this.refBook = refBook;
        this.operation = operation;
        this.lockId = lockId;
        this.userName = userName;
    }

    public RefBookOperationEntity() {
    }

    @PrePersist
    public void prePersist() {
        if (creationDate == null)
            creationDate = LocalDateTime.now(Clock.systemUTC());
    }

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

    public RefBookOperation getOperation() {
        return operation;
    }

    public void setOperation(RefBookOperation operation) {
        this.operation = operation;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

}
