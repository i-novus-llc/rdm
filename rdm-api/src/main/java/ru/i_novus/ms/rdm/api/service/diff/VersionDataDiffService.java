package ru.i_novus.ms.rdm.api.service.diff;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.VersionDataDiffCriteria;

/** Методы работы с разницей между данными. */
public interface VersionDataDiffService {

    /**
     * Поиск разницы между данными версий.
     *
     * @param criteria критерий поиска
     */
    Page<VersionDataDiff> search(VersionDataDiffCriteria criteria);

    /**
     * Проверка на опубликованность первой версии раньше второй.
     *
     * @param versionId1 идентификатор первой версии
     * @param versionId2 идентификатор второй версии
     * @return true, если версия с versionId1 опубликована раньше версии versionId2
     */
    Boolean isPublishedBefore(Integer versionId1, Integer versionId2);
}
