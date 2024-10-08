package ru.i_novus.ms.rdm.impl.entity;

import jakarta.persistence.*;
import ru.i_novus.ms.rdm.api.enumeration.FileType;

import java.util.Objects;

/**
 * Created by znurgaliev on 08.08.2018.
 */
@Entity
@Table(name = "ref_book_version_file", schema = "n2o_rdm_management" )
public class VersionFileEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "version_id", referencedColumnName = "id", nullable = false)
    private RefBookVersionEntity version;

    @Column(name = "type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private FileType type;

    @Column(name = "path", nullable = false)
    private String path;

    public VersionFileEntity() {
        // Nothing to do.
    }

    public VersionFileEntity(RefBookVersionEntity version, FileType type, String path) {

        this.version = version;
        this.type = type;
        this.path = path;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RefBookVersionEntity getVersion() {
        return version;
    }

    public void setVersion(RefBookVersionEntity version) {
        this.version = version;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionFileEntity that = (VersionFileEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(version, that.version) &&
                Objects.equals(type, that.type) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, type, path);
    }
}
