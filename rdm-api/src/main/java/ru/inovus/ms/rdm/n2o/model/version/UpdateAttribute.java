package ru.inovus.ms.rdm.n2o.model.version;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.model.UpdatableDto;
import ru.inovus.ms.rdm.n2o.util.TimeUtils;

import static ru.inovus.ms.rdm.n2o.model.version.UpdateValue.of;

public class UpdateAttribute extends UpdatableDto {

    private Integer versionId;

    // поля Structure.Attribute
    private String code;

    private UpdateValue<String> name;
    private FieldType type;
    private UpdateValue<Boolean> isPrimary;
    private UpdateValue<String> description;

    // поля Structure.Reference
    private UpdateValue<String> attribute;
    private UpdateValue<String> referenceCode;
    private UpdateValue<String> displayExpression;

    public UpdateAttribute(){}

    public UpdateAttribute(Integer versionId, Structure.Attribute attribute, Structure.Reference reference) {
        setLastActionDate(TimeUtils.nowZoned());

        this.versionId = versionId;

        //attribute fields
        this.code = attribute.getCode();
        if (attribute.getName() != null)
            this.name = of(attribute.getName());
        this.type = attribute.getType();
        if (attribute.getIsPrimary() != null)
            this.isPrimary = of(attribute.getIsPrimary());
        if (attribute.getDescription() != null)
            setDescription(of(attribute.getDescription()));

        //reference fields
        if (reference == null)
            return;
        if (reference.getAttribute() != null)
            this.attribute = of(reference.getAttribute());
        if (reference.getReferenceCode() != null)
            this.referenceCode = of(reference.getReferenceCode());
        if (reference.getDisplayExpression() != null)
            this.displayExpression = of(reference.getDisplayExpression());
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UpdateValue<String> getName() {
        return name;
    }

    public void setName(UpdateValue<String> name) {
        this.name = name;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public UpdateValue<Boolean> getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(UpdateValue<Boolean> isPrimary) {
        this.isPrimary = isPrimary;
    }

    public UpdateValue<String> getDescription() {
        return description;
    }

    public void setDescription(UpdateValue<String> description) {
        this.description = description;
    }

    public UpdateValue<String> getAttribute() {
        return attribute;
    }

    public void setAttribute(UpdateValue<String> attribute) {
        this.attribute = attribute;
    }

    public UpdateValue<String> getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(UpdateValue<String> referenceCode) {
        this.referenceCode = referenceCode;
    }

    public UpdateValue<String> getDisplayExpression() {
        return displayExpression;
    }

    public void setDisplayExpression(UpdateValue<String> displayExpression) {
        this.displayExpression = displayExpression;
    }

    public Boolean isReferenceType() {
        return FieldType.REFERENCE.equals(getType());
    }
}
