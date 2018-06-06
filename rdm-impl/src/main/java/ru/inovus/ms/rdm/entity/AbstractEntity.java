package ru.inovus.ms.rdm.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "creation_date", nullable = false)
    private Date creationDate;

    @Column(name = "last_action_date", nullable = false)
    private Date lastActionDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date versionCreationDate) {
        this.creationDate = versionCreationDate;
    }

    public Date getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(Date versionLastActionDate) {
        this.lastActionDate = versionLastActionDate;
    }

    @PrePersist
    public void prePersist() {
        Date now = new Date();

        if (creationDate == null)
            creationDate = now;

        if (lastActionDate == null)
            lastActionDate = now;
    }
}