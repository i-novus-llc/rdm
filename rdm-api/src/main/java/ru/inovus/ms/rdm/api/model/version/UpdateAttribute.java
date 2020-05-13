package ru.inovus.ms.rdm.api.model.version;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.UpdatableDto;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.inovus.ms.rdm.api.model.version.UpdateValue.of;

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

        // attribute fields
        this.code = attribute.getCode();
        this.type = attribute.getType();

        setUpdateValueIfExists(attribute::getName, this::setName);
        setUpdateValueIfExists(attribute::getIsPrimary, this::setIsPrimary);
        setUpdateValueIfExists(attribute::getDescription, this::setDescription);

        // reference fields
        if (reference == null)
            return;

        setUpdateValueIfExists(reference::getAttribute, this::setAttribute);
        setUpdateValueIfExists(reference::getReferenceCode, this::setReferenceCode);
        setUpdateValueIfExists(reference::getDisplayExpression, this::setDisplayExpression);
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

    public boolean hasIsPrimary() {
        return getIsPrimary() != null
                && getIsPrimary().isPresent()
                && Boolean.TRUE.equals(getIsPrimary().get());
    }

    public boolean isReferenceType() {
        return FieldType.REFERENCE.equals(getType());
    }

    public boolean isNullOrPresentReference() {
        return isNullOrPresent(getAttribute())
                && isNullOrPresent(getReferenceCode())
                && isNullOrPresent(getDisplayExpression());
    }

    public boolean isNotNullAndPresentReference() {
        return isNotNullAndPresent(getAttribute())
                && isNotNullAndPresent(getReferenceCode())
                && isNotNullAndPresent(getDisplayExpression());
    }

    public void fillAttribute(Structure.Attribute attribute) {

        attribute.setType(getType());

        setValueIfExists(this::getName, attribute::setName);
        setValueIfExists(this::getDescription, attribute::setDescription);
        setValueIfExists(this::getIsPrimary, attribute::setPrimary);
    }

    public void fillReference(Structure.Reference reference) {

        setValueIfExists(this::getAttribute, reference::setAttribute);
        setValueIfExists(this::getReferenceCode, reference::setReferenceCode);
        setValueIfExists(this::getDisplayExpression, reference::setDisplayExpression);
    }

    private static <T> boolean isNotNullAndPresent(UpdateValue<T> value) {
        return value != null && value.isPresent();
    }

    private static <T> boolean isNullOrPresent(UpdateValue<T> value) {
        return value == null || value.isPresent();
    }

    private static <T> void setValueIfExists(Supplier<UpdateValue<T>> getter, Consumer<T> setter) {

        UpdateValue<T> value = getter.get();
        if (value != null) {
            setter.accept(value.isPresent() ? value.get() : null);
        }
    }

    public static <T> void setUpdateValueIfExists(Supplier<T> getter, Consumer<UpdateValue<T>> setter) {

        T value = getter.get();
        if (value != null) {
            setter.accept(of(value));
        }
    }
}
