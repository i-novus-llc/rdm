package ru.inovus.ms.rdm.model;

public class Draft {
    private Integer id;
    private String storageCode;

    public Draft(Integer id, String storageCode) {
        this.id = id;
        this.storageCode = storageCode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStorageCode() {
        return storageCode;
    }

    public void setStorageCode(String storageCode) {
        this.storageCode = storageCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Draft draft = (Draft) o;

        if (id != null ? !id.equals(draft.id) : draft.id != null) return false;
        return storageCode != null ? storageCode.equals(draft.storageCode) : draft.storageCode == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (storageCode != null ? storageCode.hashCode() : 0);
        return result;
    }
}
