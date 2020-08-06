package ru.i_novus.ms.rdm.api.model.draft;

import java.util.Objects;

/**
 * Модель черновика.
 */
public class Draft {

    /** Идентификатор черновика. */
    private Integer id;

    /**
     * Код хранилища.
     */
    private String storageCode;

    /** Значение оптимистической блокировки версии-черновика. */
    private Integer optLockValue;

    public Draft(Integer id, String storageCode, Integer optLockValue) {
        this.id = id;
        this.storageCode = storageCode;
        this.optLockValue = optLockValue;
    }

    public Draft() {
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

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public boolean isVersionDraft(Integer versionId) {
        return Objects.equals(getId(), versionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Draft that = (Draft) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(storageCode, that.storageCode) &&
                Objects.equals(optLockValue, that.optLockValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storageCode, optLockValue);
    }
}
