package ru.inovus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.UpdatableDto;
import ru.inovus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.inovus.ms.rdm.api.model.version.UpdateValue.of;

@ApiModel(value = "Модель изменения атрибута черновика",
        description = "Набор входных параметров для изменения записей черновика")
public class UpdateAttributeRequest extends UpdatableDto implements DraftChangeRequest  {

    @ApiModelProperty("Идентификатор версии")
    private Integer versionId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    // Поля Structure.Attribute:
    @ApiModelProperty("Код атрибута")
    private String code;

    @ApiModelProperty("Наименование атрибута")
    private UpdateValue<String> name; // NOSONAR

    @ApiModelProperty("Тип атрибута")
    private FieldType type;

    @ApiModelProperty("Признак первичного атрибута")
    private UpdateValue<Boolean> isPrimary; // NOSONAR

    @ApiModelProperty("Описание атрибута")
    private String description;

    // Поля Structure.Reference:
    @ApiModelProperty("Код атрибута, который ссылается")
    private UpdateValue<String> attribute; // NOSONAR

    @ApiModelProperty("Код справочника, на который ссылаются")
    private UpdateValue<String> referenceCode; // NOSONAR

    @ApiModelProperty("Выражение для вычисления отображаемого ссылочного значения")
    private UpdateValue<String> displayExpression; // NOSONAR

    public UpdateAttributeRequest(){}

    public UpdateAttributeRequest(Integer versionId,
                                  Integer optLockValue,
                                  Structure.Attribute attribute,
                                  Structure.Reference reference) {

        setLastActionDate(TimeUtils.nowZoned());

        this.versionId = versionId;
        this.optLockValue = optLockValue;

        // Поля Structure.Attribute:
        this.code = attribute.getCode();
        this.type = attribute.getType();
        this.description = attribute.getDescription();

        setUpdateValueIfExists(attribute::getName, this::setName);
        setUpdateValueIfExists(attribute::getIsPrimary, this::setIsPrimary);

        // Поля Structure.Reference:
        if (reference == null)
            return;

        setUpdateValueIfExists(reference::getAttribute, this::setAttribute);
        setUpdateValueIfExists(reference::getReferenceCode, this::setReferenceCode);
        setUpdateValueIfExists(reference::getDisplayExpression, this::setDisplayExpression);
    }

    @Override
    public Integer getVersionId() {
        return versionId;
    }

    @Override
    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    @Override
    public Integer getOptLockValue() {
        return optLockValue;
    }

    @Override
    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
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
        attribute.setDescription(this.getDescription());

        setValueIfExists(this::getName, attribute::setName);
        setValueIfExists(this::getIsPrimary, attribute::setPrimary);
    }

    public void fillReference(Structure.Reference reference) {

        setValueIfExists(this::getAttribute, reference::setAttribute);
        setValueIfExists(this::getReferenceCode, reference::setReferenceCode);
        setValueIfExists(this::getDisplayExpression, reference::setDisplayExpression);
    }

    private static <T extends Serializable> boolean isNotNullAndPresent(UpdateValue<T> value) {
        return value != null && value.isPresent();
    }

    private static <T extends Serializable> boolean isNullOrPresent(UpdateValue<T> value) {
        return value == null || value.isPresent();
    }

    private static <T extends Serializable> void setValueIfExists(Supplier<UpdateValue<T>> getter,
                                                                  Consumer<T> setter) {
        UpdateValue<T> value = getter.get();
        if (value != null) {
            setter.accept(value.isPresent() ? value.get() : null);
        }
    }

    public static <T extends Serializable> void setUpdateValueIfExists(Supplier<T> getter,
                                                                       Consumer<UpdateValue<T>> setter) {
        T value = getter.get();
        if (value != null) {
            setter.accept(of(value));
        }
    }
}
