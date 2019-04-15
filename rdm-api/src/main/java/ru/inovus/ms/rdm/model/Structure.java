package ru.inovus.ms.rdm.model;

import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;


public class Structure implements Serializable {

    private List<Attribute> attributes;

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

    public Reference getReference(String attributeCode) {
        if (isEmpty(references)) {
            return null;
        }
        return references.stream().filter(reference -> reference.getAttribute().equals(attributeCode)).findAny()
                .orElse(null);
    }

    public Attribute getAttribute(String code) {
        if (isEmpty(attributes)) {
            return null;
        }
        return attributes.stream().filter(attribute -> attribute.getCode().equals(code)).findAny()
                .orElse(null);
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
        return attributes.stream().filter(attribute -> attribute.isPrimary).collect(Collectors.toList());
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

    public static class Attribute implements Serializable {

        private String code;

        private String name;

        private FieldType type;

        private Boolean isPrimary;

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

        public boolean storageEquals(Attribute a) {
            if (code != null ? !code.equals(a.code) : a.code != null)
                return false;
            if (name != null ? !name.equals(a.name) : a.name != null)
                return false;
            return type == a.type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Attribute attribute = (Attribute) o;

            if (isPrimary != attribute.isPrimary) return false;
            if (code != null ? !code.equals(attribute.code) : attribute.code != null)
                return false;
            if (name != null ? !name.equals(attribute.name) : attribute.name != null)
                return false;
            if (description != null ? !description.equals(attribute.description) : attribute.description != null)
                return false;
            return type == attribute.type;
        }

        @Override
        public int hashCode() {
            int result = code != null ? code.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (isPrimary ? 1 : 0);
            return result;
        }
    }

    public static class Reference implements Serializable {

        /**
         * Поле которое ссылается
         */
        private String attribute;

        /**
         * Веррсия на которую ссылаемся
         */
        private Integer referenceVersion;

        /**
         * Поле на которое ссылаемся
         */
        private String referenceAttribute;

        /**
         * Вид отображаемого ссылочного значения.
         * Поля справочника указываются через placeholder ${~}, например ${field}
         */
        private String displayExpression;

        public Reference() {
        }

        public Reference(String attribute, Integer referenceVersion, String referenceAttribute, String displayExpression) {
            this.attribute = attribute;
            this.referenceVersion = referenceVersion;
            this.referenceAttribute = referenceAttribute;
            this.displayExpression = displayExpression;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        public Integer getReferenceVersion() {
            return referenceVersion;
        }

        public void setReferenceVersion(Integer referenceVersion) {
            this.referenceVersion = referenceVersion;
        }

        public String getReferenceAttribute() {
            return referenceAttribute;
        }

        public void setReferenceAttribute(String referenceAttribute) {
            this.referenceAttribute = referenceAttribute;
        }

        public String getDisplayExpression() {
            return displayExpression;
        }

        public void setDisplayExpression(String displayExpression) {
            this.displayExpression = displayExpression;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Reference reference = (Reference) o;

            if (attribute != null ? !attribute.equals(reference.attribute) : reference.attribute != null) return false;
            if (referenceVersion != null ? !referenceVersion.equals(reference.referenceVersion) : reference.referenceVersion != null)
                return false;
            if (referenceAttribute != null ? !referenceAttribute.equals(reference.referenceAttribute) : reference.referenceAttribute != null)
                return false;
            return !(displayExpression != null ? !displayExpression.equals(reference.displayExpression) : reference.displayExpression != null);

        }

        @Override
        public int hashCode() {
            int result = attribute != null ? attribute.hashCode() : 0;
            result = 31 * result + (referenceVersion != null ? referenceVersion.hashCode() : 0);
            result = 31 * result + (referenceAttribute != null ? referenceAttribute.hashCode() : 0);
            result = 31 * result + (displayExpression != null ? displayExpression.hashCode() : 0);
            return result;
        }
    }

    public boolean storageEquals(Structure s) {
        return isEmpty(attributes)
                ? isEmpty(s.getAttributes())
                : attributes.stream().noneMatch(attribute -> s.attributes.stream().noneMatch(attribute::storageEquals));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Structure structure = (Structure) o;

        if (attributes != null ? !attributes.equals(structure.attributes) : structure.attributes != null) return false;
        return !(references != null ? !references.equals(structure.references) : structure.references != null);

    }

    @Override
    public int hashCode() {
        int result = attributes != null ? attributes.hashCode() : 0;
        result = 31 * result + (references != null ? references.hashCode() : 0);
        return result;
    }
}
