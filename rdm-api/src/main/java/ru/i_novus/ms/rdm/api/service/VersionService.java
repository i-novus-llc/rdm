package ru.i_novus.ms.rdm.api.service;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExistsData;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Версия справочника: Сервис.
 */
public interface VersionService {

    /**
     * Получение записей версии справочника по параметрам критерия.
     *
     * @param versionId идентификатор версии
     * @param criteria  критерий поиска
     * @return Страница записей версии справочника
     */
    Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria);

    /**
     * Получение списка версий справочника по параметрам критерия.
     *
     * @param criteria критерий поиска
     * @return Список версий справочника
     */
    Page<RefBookVersion> getVersions(VersionCriteria criteria);

    /**
     * Получение версии справочника по его идентификатору.
     *
     * @param versionId идентификатор версии
     * @return Версия справочника
     */
    RefBookVersion getById(Integer versionId);

    /**
     * Получение версии справочника по коду справочника и номеру.
     *
     * @param version     номер версии
     * @param refBookCode код справочника
     * @return Версия справочника
     */
    RefBookVersion getVersion(String version, String refBookCode);

    /**
     * Получение последней опубликованной версии справочника по коду справочника.
     *
     * @param refBookCode код справочника
     * @return Версия справочника
     */
    RefBookVersion getLastPublishedVersion(String refBookCode);

    /**
     * Получение актуальных на указанную дату записей версии справочника по коду справочника.
     *
     * @param refBookCode код справочника
     * @param date        дата актуальности записей
     * @param criteria    критерий поиска
     * @return Страница записей версии справочника
     */
    Page<RefBookRowValue> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria);

    /**
     * Получение актуальных на текущую дату записей версии справочника по коду справочника.
     *
     * @param refBookCode код справочника
     * @param criteria    критерий поиска
     * @return Страница записей версии справочника
     */
    Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria);

    /**
     * Получение структуры версии справочника.
     *
     * @param versionId идентификатор версии
     * @return Структура версии справочника
     */
    Structure getStructure(Integer versionId);

    /**
     * Получение кода хранилища версии справочника.
     *
     * @param versionId идентификатор версии
     * @return Код хранилища
     */
    String getStorageCode(Integer versionId);

    /**
     * Получение информации о существовании записей в версиях справочников.
     *
     * @param rowIds список информации о записях
     * @return Информация о существовании записей
     */
    ExistsData existsData(List<String> rowIds);

    /**
     * Получение записи версии справочника по информации о записи.
     *
     * @param rowId информация о записи в формате: <хеш записи>$<идентификатор версии>
     * @return Запись версии справочника
     */
    RefBookRowValue getRow(String rowId);

    /**
     * Выгрузка версии справочника в файл.
     *
     * @param versionId идентификатор версии
     * @param fileType  тип файла
     * @return Файл версии справочника
     */
    ExportFile getVersionFile(Integer versionId, FileType fileType);
}
