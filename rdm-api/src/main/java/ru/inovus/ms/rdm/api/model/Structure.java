package ru.inovus.ms.rdm.api.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@ApiModel("Структура")
@JsonPropertyOrder({"references", "attributes"})
public class Structure implements Serializable {

    private static final String PRIMARY_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "primary.attribute.not.found";
    private static final String PRIMARY_ATTRIBUTE_IS_MULTIPLE_EXCEPTION_CODE = "primary.attribute.is.multiple";

    @ApiParam("Атрибуты")
    private List<Attribute> attributes;

    @ApiParam("Ссылки")
    private List<Reference> references;

    public Structure() {
        this(new ArrayList<>(0), new ArrayList<>(0));
    }

    public Structure(List<Attribute> attributes, List<Reference> references) {
        this.attributes = getOrCreateList(attributes);
        this.references = getOrCreateList(references);
    }

    public Structure(Structure structure) {
        this(structure.getAttributes(), structure.getReferences());
    }

    @JsonGetter
    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = getOrCreateList(attributes);
    }

    @JsonGetter
    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = getOrCreateList(references);
    }

    public Attribute getAttribute(String code) {

        if (CollectionUtils.isEmpty(attributes))
            return null;

        return attributes.stream()
                .filter(attribute -> attribute.getCode().equals(code))
                .findAny().orElse(null);
    }

    public Reference getReference(String attributeCode) {

        if (CollectionUtils.isEmpty(references))
            return null;

        return references.stream()
                .filter(reference -> reference.getAttribute().equals(attributeCode))
                .findAny().orElse(null);
    }

    public void clearPrimary() {

        if (CollectionUtils.isEmpty(attributes))
            return;

        attributes.forEach(a -> {
            if (a.hasIsPrimary())
                a.setPrimary(Boolean.FALSE);
        });
    }

    @JsonIgnore
    public List<Attribute> getPrimary() {
        return attributes.stream()
                .filter(attribute -> attribute.isPrimary)
                .collect(toList());
    }

    /**
     * Проверка наличия первичного ключа.
     *
     * @return {@code true}, если есть хотя бы один первичный ключ, иначе - {@code false}.
     */
    public boolean hasPrimary() {
        return attributes.stream().anyMatch(attribute -> attribute.isPrimary);
    }

    /**
     * Проверка наличия структуры.
     *
     * @return {@code true}, если есть хотя бы один атрибут, иначе - {@code false}.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(attributes);
    }

    public void add(Attribute attribute, Reference reference) {

        if (attribute == null)
            return;

        if (getAttributes() == null)
            setAttributes(getOrCreateList(null));

        getAttributes().add(attribute);

        if (reference == null)
            return;

        if (getReferences() == null)
            setReferences(getOrCreateList(null));

        getReferences().add(reference);
    }

    public void update(Attribute oldAttribute, Attribute newAttribute) {

        if (oldAttribute == null || newAttribute == null)
            return;

        int index = getAttributes().indexOf(oldAttribute);
        getAttributes().set(index, newAttribute);
    }

    public void update(Reference oldReference, Reference newReference) {

        if (newReference != null) {
            if (oldReference != null) {
                int index = getReferences().indexOf(oldReference);
                getReferences().set(index, newReference);

            } else {
                getReferences().add(newReference);

            }

        } else if (oldReference != null) {
            getReferences().remove(oldReference);
        }
    }

    public void remove(String attributeCode) {

        Attribute attribute = getAttribute(attributeCode);

        if (attribute.isReferenceType())
            getReferences().remove(getReference(attributeCode));

        getAttributes().remove(attribute);
    }

    /**
     * Получение всех ссылок на справочник с указанным кодом.
     *
     * @param referenceCode код справочника, на который ссылаются
     * @return Список ссылок
     */
    public List<Reference> getRefCodeReferences(String referenceCode) {

        if (CollectionUtils.isEmpty(references))
            return emptyList();

        return references.stream()
                .filter(reference -> reference.getReferenceCode().equals(referenceCode))
                .collect(toList());
    }

    /**
     * Получение всех атрибутов-ссылок на справочник с указанным кодом.
     *
     * @param referenceCode код справочника, на который ссылаются
     * @return Список атрибутов
     */
    public List<Attribute> getRefCodeAttributes(String referenceCode) {

        if (CollectionUtils.isEmpty(attributes))
            return emptyList();

        return getRefCodeReferences(referenceCode).stream()
                .map(ref -> getAttribute(ref.getAttribute()))
                .collect(toList());
    }

    private static <T> List<T> getOrCreateList(List<T> list) {
        return list == null ? new ArrayList<>(0) : list;
    }

    @ApiModel("Атрибут справочника")
    public static class Attribute implements Serializable {

        /** Код атрибута. */
        @ApiModelProperty("Код атрибута")
        private String code;

        /** Наименование атрибута. */
        @ApiModelProperty("Наименование атрибута")
        private String name;

        /** Тип атрибута. */
        @ApiModelProperty("Тип атрибута")
        private FieldType type;

        /** Признак первичного атрибута. */
        @ApiModelProperty("Признак первичного атрибута")
        private Boolean isPrimary;

        /** Описание атрибута. */
        @ApiModelProperty("Описание атрибута")
        private String description;

        public Attribute() {
        }

        public Attribute(Attribute attribute) {
            this.code = attribute.code;
            this.name = attribute.name;
            this.type = attribute.type;
            this.isPrimary = attribute.isPrimary;
            this.description = attribute.description;
        }

        public static Attribute build(Attribute attribute) {
            if (attribute != null)
                return new Attribute(attribute);

            return new Attribute();
        }

        public static Attribute buildPrimary(String code, String name, FieldType type, String description) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(Boolean.TRUE);
            attribute.setCode(code);
            attribute.setName(name);
            attribute.setType(type);
            attribute.setDescription(description);
            return attribute;
        }

        public static Attribute build(String code, String name, FieldType type, String description) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(Boolean.FALSE);
            attribute.setCode(code);
            attribute.setName(name);
            attribute.setType(type);
            attribute.setDescription(description);
            return attribute;
        }

        @JsonGetter
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        @JsonGetter
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonGetter
        public FieldType getType() {
            return type;
        }

        public void setType(FieldType type) {
            this.type = type;
        }

        @JsonGetter
        public Boolean getIsPrimary() {
            return isPrimary;
        }

        public void setPrimary(Boolean isPrimary) {
            this.isPrimary = isPrimary != null && isPrimary;
        }

        @JsonGetter
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean hasIsPrimary() {
            return isPrimary != null && isPrimary;
        }

        @JsonIgnore
        public boolean isReferenceType() {
            return FieldType.REFERENCE.equals(getType());
        }

        /** Сравнение атрибутов только по полям, связанным с хранением данных. */
        public boolean storageEquals(Attribute that) {
            return (that != null) &&
                    Objects.equals(code, that.code) &&
                    Objects.equals(type, that.type);
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

        @Override
        public String toString() {
            return JsonUtil.getAsJson(this);
        }
    }

    @ApiModel("Ссылка на запись справочника")
    public static class Reference implements Serializable {

        /** Поле, которое ссылается. */
        @ApiModelProperty("Поле, которое ссылается")
        private String attribute;

        /** Код справочника, на который ссылаются. */
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

        public Reference(Reference reference) {
            this(reference.attribute, reference.referenceCode, reference.displayExpression);
        }

        public Reference(String attribute, String referenceCode, String displayExpression) {
            this.attribute = attribute;
            this.referenceCode = referenceCode;
            this.displayExpression = displayExpression;
        }

        public static Reference build(Reference reference) {
            if (reference != null)
                return new Reference(reference);

            return new Reference();
        }

        @JsonGetter
        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        @JsonGetter
        public String getReferenceCode() {
            return referenceCode;
        }

        public void setReferenceCode(String referenceCode) {
            this.referenceCode = referenceCode;
        }

        @JsonGetter
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
        public Attribute findReferenceAttribute(Structure referenceStructure) {

            List<Attribute> primaryAttributes = referenceStructure.getPrimary();
            if (CollectionUtils.isEmpty(primaryAttributes))
                throw new UserException(new Message(PRIMARY_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE));

            if (primaryAttributes.size() > 1)
                throw new UserException(new Message(PRIMARY_ATTRIBUTE_IS_MULTIPLE_EXCEPTION_CODE));

            return primaryAttributes.get(0);
        }

        @JsonIgnore
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

        @Override
        public String toString() {
            return JsonUtil.getAsJson(this);
        }
    }

    public boolean storageEquals(Structure that) {
        List<Attribute> others = that.getAttributes();
        return CollectionUtils.isEmpty(attributes)
                ? CollectionUtils.isEmpty(others)
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

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
