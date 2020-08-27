package ru.i_novus.ms.rdm.l10n.api.model.criteria;

import java.io.Serializable;
import java.util.Objects;

public class StorageCodeCriteria implements Serializable {

    /**
     * Исходный код хранилища.
     */
    private final String storageCode;

    /**
     * Код локали.
     */
    private final String localeCode;

    public StorageCodeCriteria(String storageCode, String localeCode) {

        this.storageCode = storageCode;
        this.localeCode = localeCode;
    }

    public String getStorageCode() {
        return storageCode;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageCodeCriteria that = (StorageCodeCriteria) o;
        return Objects.equals(localeCode, that.localeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), localeCode);
    }

    @Override
    public String toString() {
        return "L10nStorageCodeCriteria{" +
                "storageCode='" + storageCode + '\'' +
                ", localeCode=" + localeCode +
                '}';
    }
}
