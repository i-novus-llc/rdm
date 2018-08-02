package ru.inovus.ms.rdm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by znurgaliev on 30.07.2018.
 */
@Entity
@Table(name = "passport_attribute", schema = "n2o_rdm_management")
public class PassportAttributeEntity {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PassportAttributeEntity that = (PassportAttributeEntity) o;

        return !(code != null ? !code.equals(that.code) : that.code != null);

    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}
