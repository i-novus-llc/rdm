package ru.i_novus.ms.rdm.impl.entity.diff;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "ref_book_version_diff", schema = "n2o_rdm_management")
public class RefBookVersionDiffEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "old_version_id", nullable = false)
    private RefBookVersionEntity oldVersion;

    @ManyToOne
    @JoinColumn(name = "new_version_id", nullable = false)
    private RefBookVersionEntity newVersion;

    public RefBookVersionDiffEntity() {
    }

    public RefBookVersionDiffEntity(RefBookVersionEntity oldVersion, RefBookVersionEntity newVersion) {
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RefBookVersionEntity getOldVersion() {
        return oldVersion;
    }

    public void setOldVersion(RefBookVersionEntity oldVersion) {
        this.oldVersion = oldVersion;
    }

    public RefBookVersionEntity getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(RefBookVersionEntity newVersion) {
        this.newVersion = newVersion;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookVersionDiffEntity that = (RefBookVersionDiffEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(oldVersion, that.oldVersion) &&
                Objects.equals(newVersion, that.newVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, oldVersion, newVersion);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RefBookConflictEntity{");
        sb.append("id=").append(id);
        sb.append(", referrerVersion=").append(oldVersion);
        sb.append(", publishedVersion=").append(newVersion);
        sb.append('}');
        return sb.toString();
    }
}
