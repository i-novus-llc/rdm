package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.List;
import java.util.Objects;

public class Structure {

    private List<Attribute> attributes;

    private List<Reference> references;

    public Reference getReference(String attributeName) {
        if(references == null) {
            return null;
        }
        return references.stream().filter(reference -> reference.getAttribute().equals(attributeName)).findAny()
                .orElse(null);
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

    public static class Attribute {

        private String attributeName;

        private FieldType type;

        private boolean isPrimary;

        private boolean isDisplay;

        private boolean isRequired;

        public static Attribute buildPrimary(String attributeName, FieldType type, boolean isDisplay) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(true);
            attribute.setIsRequired(true);
            attribute.setAttributeName(attributeName);
            attribute.setType(type);
            attribute.setIsDisplay(isDisplay);
            return attribute;
        }

        public static Attribute build(String attributeName, FieldType type, boolean isDisplay, boolean isRequired) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(false);
            attribute.setIsRequired(isRequired);
            attribute.setAttributeName(attributeName);
            attribute.setType(type);
            attribute.setIsDisplay(isDisplay);
            return attribute;
        }


        public String getAttributeName() {
            return attributeName;
        }

        public void setAttributeName(String attributeName) {
            this.attributeName = attributeName;
        }

        public FieldType getType() {
            return type;
        }

        public void setType(FieldType type) {
            this.type = type;
        }

        public boolean isPrimary() {
            return isPrimary;
        }

        public void setPrimary(boolean primary) {
            isPrimary = primary;
        }

        public void setIsPrimary(boolean isPrimary) {
            this.isPrimary = isPrimary;
        }

        public boolean isDisplay() {
            return isDisplay;
        }

        public void setIsDisplay(boolean isDisplay) {
            this.isDisplay = isDisplay;
        }

        public boolean isRequired() {
            return isRequired;
        }

        public void setIsRequired(boolean isRequired) {
            this.isRequired = isRequired;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Attribute attribute = (Attribute) o;
            return equalsByFlags(attribute) &&
                    Objects.equals(attributeName, attribute.attributeName) &&
                    Objects.equals(type, attribute.type);
        }

        private boolean equalsByFlags(Attribute attribute) {
            return Objects.equals(isPrimary, attribute.isPrimary) &&
                    Objects.equals(isDisplay, attribute.isDisplay) &&
                    Objects.equals(isRequired, attribute.isRequired);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attributeName, type, isPrimary, isDisplay, isRequired);
        }
    }

    public static class Reference {

        /**
         * Поле которое ссылается
         */
        String attribute;

        /**
         * Веррсия на которую ссылаемся
         */
        Integer referenceVersion;

        /**
         * Поле на которое ссылаемся
         */
        String referenceAttribute;

        public Reference(String attribute, Integer referenceVersion, String referenceAttribute) {
            this.attribute = attribute;
            this.referenceVersion = referenceVersion;
            this.referenceAttribute = referenceAttribute;
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


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Reference reference = (Reference) o;
            return Objects.equals(attribute, reference.attribute) &&
                    Objects.equals(referenceVersion, reference.referenceVersion) &&
                    Objects.equals(referenceAttribute, reference.referenceAttribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attribute, referenceVersion, referenceAttribute);
        }
    }
}
