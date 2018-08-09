package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static ru.inovus.ms.rdm.model.UpdateValue.of;

public class UpdateAttribute extends UpdatableDto {

    private Integer versionId;

    // поля Structure.Attribute
    private String code;

    private UpdateValue<String> name;
    private FieldType type;
    private UpdateValue<Boolean> isPrimary;
    private UpdateValue<Boolean> isRequired;
    private UpdateValue<String> description;

    // поля Structure.Reference
    private UpdateValue<String> attribute;
    private UpdateValue<Integer> referenceVersion;
    private UpdateValue<String> referenceAttribute;
    private UpdateValue<List<String>> displayAttributes;
    private UpdateValue<List<String>> sortingAttributes;

    public UpdateAttribute(){}

    public UpdateAttribute(Integer versionId, Structure.Attribute attribute, Structure.Reference reference) {
        setLastActionDate(LocalDateTime.of(LocalDate.now(), LocalTime.now()));
        this.versionId = versionId;
        //attribute fields
        this.code = attribute.getCode();
        if (attribute.getName() != null)
            this.name = of(attribute.getName());
        this.type = attribute.getType();
        if (attribute.getIsPrimary() != null)
            this.isPrimary = of(attribute.getIsPrimary());
        if (attribute.getIsRequired() != null)
            this.isRequired = attribute.getIsPrimary() ? of(true) : of(attribute.getIsRequired());
        if (attribute.getDescription() != null)
            setDescription(of(attribute.getDescription()));
        //reference fields
        if (reference == null)
            return;
        if (reference.getAttribute() != null)
            this.attribute = of(reference.getAttribute());
        if (reference.getReferenceVersion() != null)
            this.referenceVersion = of(reference.getReferenceVersion());
        if (reference.getReferenceAttribute() != null)
            this.referenceAttribute = of(reference.getReferenceAttribute());
        if (reference.getDisplayAttributes() != null)
            this.displayAttributes = of(reference.getDisplayAttributes());
        if (reference.getSortingAttributes() != null)
            this.sortingAttributes = of(reference.getSortingAttributes());
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
        if (isPrimary != null && isPrimary.isPresent() && isPrimary.get())
            setIsRequired(of(true));
    }

    public UpdateValue<Boolean> getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(UpdateValue<Boolean> isRequired) {
        if (isPrimary != null && isPrimary.isPresent() && isPrimary.get() && isRequired != null && isRequired.isPresent() && !isRequired.get())
            throw new IllegalStateException("primary attribute must be required");
        this.isRequired = isRequired;
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

    public UpdateValue<Integer> getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(UpdateValue<Integer> referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public UpdateValue<String> getReferenceAttribute() {
        return referenceAttribute;
    }

    public void setReferenceAttribute(UpdateValue<String> referenceAttribute) {
        this.referenceAttribute = referenceAttribute;
    }

    public UpdateValue<List<String>> getDisplayAttributes() {
        return displayAttributes;
    }

    public void setDisplayAttributes(UpdateValue<List<String>> displayAttributes) {
        this.displayAttributes = displayAttributes;
    }

    public UpdateValue<List<String>> getSortingAttributes() {
        return sortingAttributes;
    }

    public void setSortingAttributes(UpdateValue<List<String>> sortingAttributes) {
        this.sortingAttributes = sortingAttributes;
    }
}
