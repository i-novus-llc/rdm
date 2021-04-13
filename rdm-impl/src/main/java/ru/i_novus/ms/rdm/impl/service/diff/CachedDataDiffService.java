package ru.i_novus.ms.rdm.impl.service.diff;

import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookAttributeDiff;
import ru.i_novus.platform.datastorage.temporal.model.DataDifference;

public interface CachedDataDiffService {

    /**
     * Поиск разницы между данными по критерию сравнения.
     *
     * @param criteria             критерий сравнения данных
     * @param refBookAttributeDiff разница между атрибутами
     * @return Разница между данными
     */
    DataDifference getCachedDataDifference(CompareDataCriteria criteria, RefBookAttributeDiff refBookAttributeDiff);
}
