package ru.inovus.ms.rdm.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ref_book", schema = "n2o_rdm_management")
public class RefBookEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "category")
    private String category;

    @Column(name = "removable", nullable = false)
    private Boolean removable;

    @Column(name = "archived", nullable = false)
    private Boolean archived;

    @OneToMany(mappedBy="refBook", cascade = CascadeType.ALL)
    List<RefBookVersionEntity> versionList = new ArrayList<>();

    @OneToOne(mappedBy = "refBook")
    RefBookOperationEntity currentOperation;

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

    public void setCurrentOperation(RefBookOperationEntity currentOperation) {
        this.currentOperation = currentOperation;
    }

    public RefBookOperationEntity getCurrentOperation() {
        return currentOperation;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookEntity that = (RefBookEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(removable, that.removable) &&
                Objects.equals(archived, that.archived) &&
                Objects.equals(currentOperation, that.currentOperation) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, removable, archived, currentOperation, category);
    }
}