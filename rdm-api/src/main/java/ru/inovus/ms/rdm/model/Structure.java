package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@ApiModel("Структура")
public class Structure implements Serializable {

    private static final String PRIMARY_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "primary.attribute.not.found";
    private static final String PRIMARY_ATTRIBUTE_IS_MULTIPLE_EXCEPTION_CODE = "primary.attribute.is.multiple";

    @ApiModelProperty("Атрибуты")
    private List<Attribute> attributes;

    @ApiModelProperty("Ссылки")
    private List<Reference> references;

    public Structure() {
    }

    public Structure(List<Attribute> attributes, List<Reference> references) {
        this.attributes = attributes;
        this.references = references;
    }

    public Structure(Structure other) {
        this(other.getAttributes(), other.getReferences());
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public Attribute getAttribute(String code) {
        if (isEmpty(attributes)) {
            return null;
        }
        return attributes.stream()
                .filter(attribute -> attribute.getCode().equals(code))
                .findAny().orElse(null);
    }

    public Reference getReference(String attributeCode) {
        if (isEmpty(references)) {
            return null;
        }
        return references.stream()
                .filter(reference -> reference.getAttribute().equals(attributeCode))
                .findAny().orElse(null);
    }

    public void clearPrimary() {
        if (isEmpty(attributes)) {
            return;
        }
        attributes.forEach(a -> {
            if (a.getIsPrimary())
                a.setPrimary(false);
        });
    }

    public List<Attribute> getPrimary() {
        return attributes.stream()
                .filter(attribute -> attribute.isPrimary)
                .collect(Collectors.toList());
    }

    /**
     * Получение всех ссылок на справочник с указанным кодом.
     *
     * @param referenceCode код справочника, на который ссылаются
     * @return Список ссылок
     */
    public List<Reference> getRefCodeReferences(String referenceCode) {
        if (isEmpty(references)) {
            return Collections.emptyList();
        }
        return references.stream()
                .filter(reference -> reference.getReferenceCode().equals(referenceCode))
                .collect(Collectors.toList());
    }

    /**
     * Получение всех атрибутов-ссылок на справочник с указанным кодом.
     *
     * @param referenceCode код справочника, на который ссылаются
     * @return Список атрибутов
     */
    public List<Attribute> getRefCodeAttributes(String referenceCode) {
        if (isEmpty(attributes)) {
            return Collections.emptyList();
        }
        return getRefCodeReferences(referenceCode).stream()
                .map(ref -> getAttribute(ref.getAttribute()))
                .collect(toList());
    }

    @ApiModel("Атрибут справочника")
    public static class Attribute implements Serializable {

        /**
         * Код атрибута.
         */
        @ApiModelProperty("Код атрибута")
        private String code;

        /**
         * Наименование атрибута.
         */
        @ApiModelProperty("Наименование атрибута")
        private String name;

        /**
         * Тип атрибута.
         */
        @ApiModelProperty("Тип атрибута")
        private FieldType type;

        /**
         * Признак первичного атрибута.
         */
        @ApiModelProperty("Признак первичного атрибута")
        private Boolean isPrimary;

        /**
         * Описание атрибута.
         */
        @ApiModelProperty("Описание атрибута")
        private String description;

        public static Attribute buildPrimary(String code, String name, FieldType type, String description) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(true);
            attribute.setCode(code);
            attribute.setName(name);
            attribute.setType(type);
            attribute.setDescription(description);
            return attribute;
        }

        public static Attribute build(String code, String name, FieldType type, String description) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(false);
            attribute.setCode(code);
            attribute.setName(name);
            attribute.setType(type);
            attribute.setDescription(description);
            return attribute;
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

        public FieldType getType() {
            return type;
        }

        public void setType(FieldType type) {
            this.type = type;
        }

        public Boolean getIsPrimary() {
            return isPrimary;
        }

        public void setPrimary(Boolean isPrimary) {
            this.isPrimary = isPrimary != null && isPrimary;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean storageEquals(Attribute that) {
            return Objects.equals(code, that.code) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(type, that.type);
        }

        public Boolean isReferenceType() {
            return FieldType.REFERENCE.equals(getType());
        }

        @Override
        @SuppressWarnings("squid:S1067")
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Attribute that = (Attribute) o;
            return Objects.equals(isPrimary, that.isPrimary) &&
                    Objects.equals(code, that.code) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code, name, type, isPrimary);
        }
    }

    @ApiModel("Ссылка")
    public static class Reference implements Serializable {

        /**
         * Поле, которое ссылается.
         */
        @ApiModelProperty("Поле, которое ссылается")
        private String attribute;

        /**
         * Код справочника, на который ссылаются.
         */
        @ApiModelProperty("Код справочника, на который ссылаются")
        private String referenceCode;

        /**
         * Выражение для вычисления отображаемого ссылочного значения.
         * Поля справочника указываются через placeholder ${~}, например ${field}
         */
        @ApiModelProperty("Выражение для вычисления отображаемого ссылочного значения")
        private String displayExpression;

        public Reference() {
        }

        public Reference(String attribute, String referenceCode, String displayExpression) {
            this.attribute = attribute;
            this.referenceCode = referenceCode;
            this.displayExpression = displayExpression;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        public String getReferenceCode() {
            return referenceCode;
        }

        public void setReferenceCode(String referenceCode) {
            this.referenceCode = referenceCode;
        }

        public String getDisplayExpression() {
            return displayExpression;
        }

        public void setDisplayExpression(String displayExpression) {
            this.displayExpression = displayExpression;
        }

        /**
         * Поиск атрибута в справочнике, на который ссылаются.
         * В текущей реализации это может быть только первичный ключ версии справочника.
         *
         * @param referenceStructure структура версии справочника, на который ссылаются
         * @return Атрибут версии справочника
         */
        public Structure.Attribute findReferenceAttribute(Structure referenceStructure) {

            List<Structure.Attribute> primaryAttributes = referenceStructure.getPrimary();
            if (isEmpty(primaryAttributes))
                throw new UserException(new Message(PRIMARY_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE));
            if (primaryAttributes.size() > 1)
                throw new UserException(new Message(PRIMARY_ATTRIBUTE_IS_MULTIPLE_EXCEPTION_CODE));

            return primaryAttributes.get(0);
        }

        public boolean isNull() {
            return StringUtils.isEmpty(attribute) || StringUtils.isEmpty(referenceCode);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Reference that = (Reference) o;
            return Objects.equals(attribute, that.attribute) &&
                    Objects.equals(referenceCode, that.referenceCode) &&
                    Objects.equals(displayExpression, that.displayExpression);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attribute, referenceCode, displayExpression);
        }
    }

    public boolean storageEquals(Structure that) {
        List<Attribute> others = that.getAttributes();
        return isEmpty(attributes)
                ? isEmpty(others)
                : attributes.size() == others.size()
                && attributes.stream().noneMatch(attribute -> others.stream().noneMatch(attribute::storageEquals));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Structure that = (Structure) o;
        return  Objects.equals(attributes, that.attributes) &&
                Objects.equals(references, that.references);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, references);
    }
}
