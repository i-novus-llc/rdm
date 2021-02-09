package ru.i_novus.ms.rdm.api.service;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiff;

public interface VersionDataDiffService {

    /**
     * Проверка на опубликованность первой версии раньше второй.
     *
     * @param id1 идентификатор первой версии
     * @param id2 идентификатор второй версии
     * @return true, если версия с id1 опубликована раньше версии id2
     */
    Boolean isPublishedBefore(Integer id1, Integer id2);

    /**
     * Получение разницы между данными версий.
     */
    Page<VersionDataDiff> search(CompareDataCriteria criteria);

    /**
     * Сохранение результата сравнения после публикации справочника.
     * 
     * @param refBookCode код справочника
     */
    void saveLastVersionDataDiff(String refBookCode);
}
