package ru.i_novus.ms.rdm.impl.entity.diff;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "version_data_diff", schema = "diff")
public class VersionDataDiffEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "version_diff_id", nullable = false)
    private RefBookVersionDiffEntity versionDiffEntity;

    @Column(name = "primaries", nullable = false)
    private String primaries;

    @Column(name = "values", nullable = false)
    private String values;

    public VersionDataDiffEntity() {
        // Nothing to do.
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RefBookVersionDiffEntity getVersionDiffEntity() {
        return versionDiffEntity;
    }

    public void setVersionDiffEntity(RefBookVersionDiffEntity versionDiffEntity) {
        this.versionDiffEntity = versionDiffEntity;
    }

    public String getPrimaries() {
        return primaries;
    }

    public void setPrimaries(String primaries) {
        this.primaries = primaries;
    }

    public String getValues() {
        return values;
    }

    public void setValues(String values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionDataDiffEntity that = (VersionDataDiffEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(versionDiffEntity, that.versionDiffEntity) &&
                Objects.equals(primaries, that.primaries) &&
                Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, versionDiffEntity, primaries, values);
    }
}
