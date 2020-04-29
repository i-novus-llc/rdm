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

        //attribute fields
        this.code = attribute.getCode();
        this.type = attribute.getType();

        setUpdateValueIfPresent(attribute::getName, this::setName);
        setUpdateValueIfPresent(attribute::getIsPrimary, this::setIsPrimary);
        setUpdateValueIfPresent(attribute::getDescription, this::setDescription);

        //reference fields
        if (reference == null)
            return;

        setUpdateValueIfPresent(reference::getAttribute, this::setAttribute);
        setUpdateValueIfPresent(reference::getReferenceCode, this::setReferenceCode);
        setUpdateValueIfPresent(reference::getDisplayExpression, this::setDisplayExpression);
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

    public void fillAttribute(Structure.Attribute attribute) {

        setValueIfPresent(this::getName, attribute::setName);
        setValueIfPresent(this::getDescription, attribute::setDescription);
        setValueIfPresent(this::getIsPrimary, attribute::setPrimary);

        attribute.setType(getType());
    }

    public void fillReference(Structure.Reference reference) {

        setValueIfPresent(this::getAttribute, reference::setAttribute);
        setValueIfPresent(this::getReferenceCode, reference::setReferenceCode);
        setValueIfPresent(this::getDisplayExpression, reference::setDisplayExpression);
    }

    private static <T> void setValueIfPresent(Supplier<UpdateValue<T>> valueGetter, Consumer<T> valueSetter) {

        UpdateValue<T> value = valueGetter.get();
        if (value != null) {
            valueSetter.accept(value.isPresent() ? value.get() : null);
        }
    }

    private static <T> void setUpdateValueIfPresent(Supplier<T> valueGetter, Consumer<UpdateValue<T>> valueSetter) {

        T value = valueGetter.get();
        if (value != null) {
            valueSetter.accept(of(value));
        }
    }
}
