package ru.inovus.ms.rdm.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ref_book", schema = "n2o_rdm_management")
public class RefBookEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "removable", nullable = false)
    private Boolean removable;

    @Column(name = "archived", nullable = false)
    private Boolean archived;

    @OneToMany(mappedBy="refBook", cascade = CascadeType.ALL)
    List<RefBookVersionEntity> versionList = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public List<RefBookVersionEntity> getVersionList() {
        return versionList;
    }

    public void setVersionList(List<RefBookVersionEntity> versionList) {
        this.versionList = versionList;
    }
}