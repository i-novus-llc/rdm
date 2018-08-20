package ru.inovus.ms.rdm.entity;

import javax.persistence.*;

/**
 * Created by znurgaliev on 30.07.2018.
 */
@Entity
@Table(name = "passport_attribute", schema = "n2o_rdm_management")
public class PassportAttributeEntity {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "position" )
    private Integer position;

    public PassportAttributeEntity() {
    }

    public PassportAttributeEntity(String code) {
        this.code = code;
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
}
