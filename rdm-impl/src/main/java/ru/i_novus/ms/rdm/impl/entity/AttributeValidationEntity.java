package ru.i_novus.ms.rdm.impl.entity;

import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;

import javax.persistence.*;

/**
 * Created by znurgaliev on 20.11.2018.
 */
@Entity
@Table(name = "attribute_validation", schema = "n2o_rdm_management")
public class AttributeValidationEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "version_id", referencedColumnName = "id", nullable = false)
    private RefBookVersionEntity version;

    @Column(name = "attribute", nullable = false)
    private String attribute;

    @Column(name = "type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private AttributeValidationType type;

    @Column(name = "value")
    private String value;

    public AttributeValidationEntity() {
    }

    public AttributeValidationEntity(RefBookVersionEntity version, String attribute, AttributeValidationType type, String value) {
        this.version = version;
        this.attribute = attribute;
        this.type = type;
        this.value = value;
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

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public AttributeValidationType getType() {
        return type;
    }

    public void setType(AttributeValidationType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AttributeValidation toModel() {

        AttributeValidation model = this.getType().getValidationInstance();
        model.valueFromString(this.getValue());
        model.setVersionId(this.getVersion().getId());
        model.setAttribute(this.getAttribute());

        return model;
    }
}
