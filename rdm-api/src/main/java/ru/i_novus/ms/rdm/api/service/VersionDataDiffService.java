package ru.i_novus.ms.rdm.api.service;

public interface VersionDataDiffService {

    /**
     * Сохранение результата сравнения после публикации справочника
     * 
     * @param refBookCode код справочника
     */
    void saveLastVersionDataDiff(String refBookCode);
}
