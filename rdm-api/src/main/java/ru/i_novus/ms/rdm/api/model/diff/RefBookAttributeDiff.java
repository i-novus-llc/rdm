package ru.i_novus.ms.rdm.api.model.diff;

import java.util.List;

/** Разница между атрибутами версий справочника. */
public class RefBookAttributeDiff {

    private List<String> oldAttributes;
    private List<String> newAttributes;
    private List<String> updatedAttributes;

    public RefBookAttributeDiff() {
        // Nothing to do.
    }

    public RefBookAttributeDiff(List<String> oldAttributes, List<String> newAttributes, List<String> updatedAttributes) {
        this.oldAttributes = oldAttributes;
        this.newAttributes = newAttributes;
        this.updatedAttributes = updatedAttributes;
    }

    public List<String> getOldAttributes() {
        return oldAttributes;
    }

    public void setOldAttributes(List<String> oldAttributes) {
        this.oldAttributes = oldAttributes;
    }

    public List<String> getNewAttributes() {
        return newAttributes;
    }

    public void setNewAttributes(List<String> newAttributes) {
        this.newAttributes = newAttributes;
    }

    public List<String> getUpdatedAttributes() {
        return updatedAttributes;
    }

    public void setUpdatedAttributes(List<String> updatedAttributes) {
        this.updatedAttributes = updatedAttributes;
    }
}