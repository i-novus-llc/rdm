package ru.i_novus.ms.rdm.api.service;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiff;

public interface VersionDataDiffService {

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

    /**
     * Проверка на опубликованность первой версии раньше второй.
     *
     * @param versionId1 идентификатор первой версии
     * @param versionId2 идентификатор второй версии
     * @return true, если версия с versionId1 опубликована раньше версии versionId2
     */
    Boolean isPublishedBefore(Integer versionId1, Integer versionId2);
}
