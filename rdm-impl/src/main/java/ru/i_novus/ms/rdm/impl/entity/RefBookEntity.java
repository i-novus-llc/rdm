package ru.i_novus.ms.rdm.impl.entity;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ref_book", schema = "n2o_rdm_management")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public class RefBookEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", insertable = false, updatable = false)
    private RefBookTypeEnum type;

    @Column(name = "category")
    private String category;

    @Column(name = "removable", nullable = false)
    private Boolean removable;

    @Column(name = "archived", nullable = false)
    private Boolean archived;

    @OneToMany(mappedBy="refBook", cascade = CascadeType.ALL)
    private List<RefBookVersionEntity> versionList = new ArrayList<>();

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

    public RefBookTypeEnum getType() {
        return type;
    }

    public void setType(RefBookTypeEnum type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public RefBookVersionEntity createChangeableVersion() {
        return null;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookEntity that = (RefBookEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(type, that.type) &&
                Objects.equals(category, that.category) &&
                Objects.equals(removable, that.removable) &&
                Objects.equals(archived, that.archived);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, type, category, removable, archived);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RefBookEntity{");
        sb.append("id=").append(id);
        sb.append(", code='").append(code).append('\'');
        sb.append(", type=").append(type);
        sb.append(", category='").append(category).append('\'');
        sb.append(", removable=").append(removable);
        sb.append(", archived=").append(archived);
        sb.append(", versionList=").append(versionList);
        sb.append('}');
        return sb.toString();
    }
}