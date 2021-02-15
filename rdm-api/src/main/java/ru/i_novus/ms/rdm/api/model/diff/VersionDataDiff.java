package ru.i_novus.ms.rdm.api.model.diff;

import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.Objects;

/**
 * Разница между данными версий: Модель.
 * <p>
 * Версии не обязательно следуют друг за другом.
 * В этом случае нужны два diff: первый и последний.
 */
public class VersionDataDiff {

    private String primaryValues;

    private DiffRowValue firstDiffRowValue;

    private DiffRowValue lastDiffRowValue;

    public VersionDataDiff(String primaryValues, DiffRowValue firstDiffRowValue, DiffRowValue lastDiffRowValue) {

        this.primaryValues = primaryValues;
        this.firstDiffRowValue = firstDiffRowValue;
        this.lastDiffRowValue = lastDiffRowValue;
    }

    public String getPrimaryValues() {
        return primaryValues;
    }

    public void setPrimaryValues(String primaryValues) {
        this.primaryValues = primaryValues;
    }

    public DiffRowValue getFirstDiffRowValue() {
        return firstDiffRowValue;
    }

    public void setFirstDiffRowValue(DiffRowValue firstDiffRowValue) {
        this.firstDiffRowValue = firstDiffRowValue;
    }

    public DiffRowValue getLastDiffRowValue() {
        return lastDiffRowValue;
    }

    public void setLastDiffRowValue(DiffRowValue lastDiffRowValue) {
        this.lastDiffRowValue = lastDiffRowValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionDataDiff)) return false;

        VersionDataDiff dataDiff = (VersionDataDiff) o;
        return Objects.equals(primaryValues, dataDiff.primaryValues) &&
                Objects.equals(firstDiffRowValue, dataDiff.firstDiffRowValue) &&
                Objects.equals(lastDiffRowValue, dataDiff.lastDiffRowValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryValues, firstDiffRowValue, lastDiffRowValue);
    }
}
