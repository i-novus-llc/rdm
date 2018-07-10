package ru.inovus.ms.rdm.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

public class Structure implements Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);

    private List<Attribute> attributes;

    private List<Reference> references;

    public Reference getReference(String attributeName) {
        if (isEmpty(references)) {
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

    public static class Attribute implements Cloneable {

        private String name;

        private FieldType type;

        private boolean isPrimary;

        private boolean isRequired;

        public static Attribute buildPrimary(String attributeName, FieldType type) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(true);
            attribute.setIsRequired(true);
            attribute.setName(attributeName);
            attribute.setType(type);
            return attribute;
        }

        public static Attribute build(String attributeName, FieldType type, boolean isRequired) {
            Attribute attribute = new Attribute();
            attribute.setPrimary(false);
            attribute.setIsRequired(isRequired);
            attribute.setName(attributeName);
            attribute.setType(type);
            return attribute;
        }

        @Override
        protected Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                logger.debug("failed to clone attribute");
            }
            return null;
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

        public boolean isPrimary() {
            return isPrimary;
        }

        public void setPrimary(boolean primary) {
            isPrimary = primary;
        }

        public void setIsPrimary(boolean isPrimary) {
            this.isPrimary = isPrimary;
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

            if (isPrimary != attribute.isPrimary) return false;
            if (isRequired != attribute.isRequired) return false;
            if (name != null ? !name.equals(attribute.name) : attribute.name != null)
                return false;
            return type == attribute.type;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (isPrimary ? 1 : 0);
            result = 31 * result + (isRequired ? 1 : 0);
            return result;
        }
    }

    public static class Reference implements Cloneable {

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

        List<String> displayAttributes;

        public Reference(String attribute, Integer referenceVersion, String referenceAttribute, List<String> displayAttributes) {
            this.attribute = attribute;
            this.referenceVersion = referenceVersion;
            this.referenceAttribute = referenceAttribute;
            this.displayAttributes = displayAttributes;
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
            return displayAttributes;
        }

        public void setDisplayAttributes(List<String> displayAttributes) {
            this.displayAttributes = displayAttributes;
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
            return !(displayAttributes != null ? !displayAttributes.equals(reference.displayAttributes) : reference.displayAttributes != null);

        }

        @Override
        public int hashCode() {
            int result = attribute != null ? attribute.hashCode() : 0;
            result = 31 * result + (referenceVersion != null ? referenceVersion.hashCode() : 0);
            result = 31 * result + (referenceAttribute != null ? referenceAttribute.hashCode() : 0);
            result = 31 * result + (displayAttributes != null ? displayAttributes.hashCode() : 0);
            return result;
        }

        @Override
        protected Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                logger.debug("failed to clone attribute");
            }
            return null;
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

    @Override
    public Object clone() throws CloneNotSupportedException {
        Structure clonedObj = (Structure) super.clone();
        List<Attribute> attributes = new ArrayList<>();
        List<Reference> references = new ArrayList<>();
        if (this.attributes != null) {
            this.attributes.forEach(attribute -> attributes.add((Attribute) attribute.clone()));
            clonedObj.setAttributes(attributes);
        }
        if (this.references != null) {
            this.references.forEach(reference -> references.add((Reference) reference.clone()));
            clonedObj.setReferences(references);
        }
        return clonedObj;
    }
}
