package ru.i_novus.ms.rdm.impl.entity.diff;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Разница между данными версий: Результат поиска.
 * <p>
 * Версии не обязательно следуют друг за другом.
 * В этом случае нужны два diff: первый и последний.
 */
@Entity
public class VersionDataDiffResult {

    @Id
    @Column(name = "primary_values")
    private String primaryValues;

    @Column(name = "first_diff_values")
    private String firstDiffValues;

    @Column(name = "last_diff_values")
    private String lastDiffValues;

    public VersionDataDiffResult() {
        // Nothing to do.
    }

    public String getPrimaryValues() {
        return primaryValues;
    }

    public void setPrimaryValues(String primaryValues) {
        this.primaryValues = primaryValues;
    }

    public String getFirstDiffValues() {
        return firstDiffValues;
    }

    public void setFirstDiffValues(String firstDiffValues) {
        this.firstDiffValues = firstDiffValues;
    }

    public String getLastDiffValues() {
        return lastDiffValues;
    }

    public void setLastDiffValues(String lastDiffValues) {
        this.lastDiffValues = lastDiffValues;
    }
}
