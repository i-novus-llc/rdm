package ru.i_novus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.UpdatableDto;
import ru.i_novus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.i_novus.ms.rdm.api.model.version.UpdateValue.of;

@ApiModel(value = "Модель изменения атрибута черновика",
        description = "Набор входных параметров для изменения атрибута черновика")
public class UpdateAttributeRequest extends UpdatableDto implements DraftChangeRequest  {

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    // Поля Structure.Attribute:
    @ApiModelProperty("Код атрибута")
    private String code;

    @ApiModelProperty("Наименование атрибута")
    private UpdateValue<String> name;

    @ApiModelProperty("Тип атрибута")
    private FieldType type;

    @ApiModelProperty("Признак первичного атрибута")
    private UpdateValue<Boolean> isPrimary;

    @ApiModelProperty("Признак переводимого атрибута")
    private UpdateValue<Boolean> localizable;

    @ApiModelProperty("Описание атрибута")
    private String description;

    // Поля Structure.Reference:
    @ApiModelProperty("Код атрибута, который ссылается")
    private UpdateValue<String> attribute;

    @ApiModelProperty("Код справочника, на который ссылаются")
    private UpdateValue<String> referenceCode;

    @ApiModelProperty("Выражение для вычисления отображаемого ссылочного значения")
    private UpdateValue<String> displayExpression;

    @ApiParam("Пользовательские проверки для атрибута")
    private List<AttributeValidation> validations;

    public UpdateAttributeRequest() {
        // Nothing to do.
    }

    public UpdateAttributeRequest(Integer optLockValue,
                                  Structure.Attribute attribute,
                                  Structure.Reference reference) {

        setLastActionDate(TimeUtils.nowZoned());

        this.optLockValue = optLockValue;

        // Поля Structure.Attribute:
        this.code = attribute.getCode();
        this.type = attribute.getType();
        this.description = attribute.getDescription();

        setUpdateValueIfExists(attribute::getName, this::setName);
        setUpdateValueIfExists(attribute::getIsPrimary, this::setIsPrimary);
        setUpdateValueIfExists(attribute::getLocalizable, this::setLocalizable);

        // Поля Structure.Reference:
        if (reference == null)
            return;

        setUpdateValueIfExists(reference::getAttribute, this::setAttribute);
        setUpdateValueIfExists(reference::getReferenceCode, this::setReferenceCode);
        setUpdateValueIfExists(reference::getDisplayExpression, this::setDisplayExpression);
    }

    public UpdateAttributeRequest(Integer optLockValue,
                                  Structure.Attribute attribute,
                                  Structure.Reference reference,
                                  List<AttributeValidation> validations) {
        this(optLockValue, attribute, reference);

        this.validations = validations;
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

    public UpdateValue<Boolean> getLocalizable() {
        return localizable;
    }

    public void setLocalizable(UpdateValue<Boolean> localizable) {
        this.localizable = localizable;
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

    public List<AttributeValidation> getValidations() {
        return validations;
    }

    public void setValidations(List<AttributeValidation> validations) {
        this.validations = validations;
    }

    public boolean hasIsPrimary() {

        return getIsPrimary() != null
                && getIsPrimary().isPresent()
                && Boolean.TRUE.equals(getIsPrimary().get());
    }

    public boolean isLocalizable() {

        return getLocalizable() != null
                && getLocalizable().isPresent()
                && Boolean.TRUE.equals(getLocalizable().get());
    }

    public boolean isReferenceType() {
        return FieldType.REFERENCE.equals(getType());
    }

    public boolean isReferenceUpdating() {

        return isNullOrPresent(getAttribute())
                && isNullOrPresent(getReferenceCode())
                && isNullOrPresent(getDisplayExpression());
    }

    public boolean isReferenceFilling() {

        return isNotNullAndPresent(getAttribute())
                && isNotNullAndPresent(getReferenceCode())
                && isNotNullAndPresent(getDisplayExpression());
    }

    public void fillAttribute(Structure.Attribute attribute) {

        attribute.setType(getType());
        setValueIfExists(this::getName, attribute::setName);

        setValueIfExists(this::getIsPrimary, attribute::setIsPrimary);
        setValueIfExists(this::getLocalizable, attribute::setLocalizable);
        attribute.setDescription(this.getDescription());
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
