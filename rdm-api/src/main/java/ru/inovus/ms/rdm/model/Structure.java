package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

public class Structure implements Serializable {

    private List<Attribute> attributes;

    private List<Reference> references;

    public Structure() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public Structure(List<Attribute> attributes, List<Reference> references) {
        this.attributes = attributes;
        this.references = references;
    }

    public Structure(Structure other) {
        this(other.getAttributes(), other.getReferences());
    }

    public Reference getReference(String attributeName) {
        if (isEmpty(references)) {
            return null;
        }
        return references.stream().filter(reference -> reference.getAttribute().equals(attributeName)).findAny()
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

        private Boolean isRequired;

        private String description;

        public static Attribute buildPrimary(String code, String name, FieldType type, String description) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(true);
            attribute.setIsRequired(true);
            attribute.setCode(code);
            attribute.setName(name);
            attribute.setType(type);
            attribute.setDescription(description);
            return attribute;
        }

        public static Attribute build(String code, String name, FieldType type, boolean isRequired, String description) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(false);
            attribute.setIsRequired(isRequired);
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
            this.isPrimary = isPrimary != null ? isPrimary : false;
        }

        public Boolean getIsRequired() {
            return isRequired;
        }

        public void setIsRequired(Boolean isRequired) {
            this.isRequired = isRequired != null ? isRequired : false;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Attribute attribute = (Attribute) o;

            if (isPrimary != attribute.isPrimary) return false;
            if (isRequired != attribute.isRequired) return false;
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
            result = 31 * result + (isRequired ? 1 : 0);
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

        private List<String> displayAttributes;

        private List<String> sortingAttributes;

        public Reference() {
        }

        public Reference(String attribute, Integer referenceVersion, String referenceAttribute, List<String> displayAttributes, List<String> sortingAttributes) {
            this.attribute = attribute;
            this.referenceVersion = referenceVersion;
            this.referenceAttribute = referenceAttribute;
            this.displayAttributes = displayAttributes;
            this.sortingAttributes = sortingAttributes != null ? sortingAttributes : displayAttributes;
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

        public List<String> getDisplayAttributes() {
            return displayAttributes != null ? displayAttributes : singletonList(referenceAttribute);
        }

        public void setDisplayAttributes(List<String> displayAttributes) {
            this.displayAttributes = displayAttributes;
        }

        public List<String> getSortingAttributes() {
            return sortingAttributes != null ? sortingAttributes : getDisplayAttributes();
        }

        public void setSortingAttributes(List<String> sortingAttributes) {
            this.sortingAttributes = sortingAttributes;
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
            if (sortingAttributes != null ? !sortingAttributes.equals(reference.sortingAttributes) : reference.sortingAttributes != null)
                return false;
            return !(displayAttributes != null ? !displayAttributes.equals(reference.displayAttributes) : reference.displayAttributes != null);

        }

        @Override
        public int hashCode() {
            int result = attribute != null ? attribute.hashCode() : 0;
            result = 31 * result + (referenceVersion != null ? referenceVersion.hashCode() : 0);
            result = 31 * result + (referenceAttribute != null ? referenceAttribute.hashCode() : 0);
            result = 31 * result + (displayAttributes != null ? displayAttributes.hashCode() : 0);
            result = 31 * result + (sortingAttributes != null ? sortingAttributes.hashCode() : 0);
            return result;
        }
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
