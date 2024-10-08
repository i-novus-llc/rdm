package ru.i_novus.ms.rdm.impl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * Created by znurgaliev on 30.07.2018.
 */
@Entity
@Table(name = "passport_attribute", schema = "n2o_rdm_management")
public class PassportAttributeEntity implements Serializable {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "position" )
    private Integer position;

    @Column(name = "comparable")
    private Boolean comparable;

    public PassportAttributeEntity() {
    }

    public PassportAttributeEntity(String code) {
        this.code = code;
    }

    public PassportAttributeEntity(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Boolean getComparable() {
        return comparable;
    }

    public void setComparable(Boolean comparable) {
        this.comparable = comparable;
    }
}
