package ru.inovus.ms.rdm.entity;

import javax.persistence.*;
import java.util.Objects;

/**
 * Created by znurgaliev on 30.07.2018.
 */
@Entity
@Table(name = "passport_value", schema = "n2o_rdm_management")
public class PassportValueEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "code", referencedColumnName = "code", nullable = false)
    private PassportAttributeEntity attribute;

    @Column(name = "value", nullable = true)
    private String value;

    @ManyToOne
    @JoinColumn(name = "version_id", referencedColumnName = "id", nullable = false)
    private RefBookVersionEntity version;

    public PassportValueEntity(PassportAttributeEntity attribute, String value, RefBookVersionEntity version) {
        this.attribute = attribute;
        this.value = value;
        this.version = version;
    }

    public PassportValueEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public PassportAttributeEntity getAttribute() {
        return attribute;
    }

    public void setAttribute(PassportAttributeEntity attribute) {
        this.attribute = attribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RefBookVersionEntity getVersion() {
        return version;
    }

    public void setVersion(RefBookVersionEntity version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassportValueEntity entity = (PassportValueEntity) o;
        return Objects.equals(id, entity.id) &&
                Objects.equals(attribute, entity.attribute) &&
                Objects.equals(value, entity.value) &&
                Objects.equals(version, entity.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, attribute, value, version);
    }
}
