package ru.i_novus.ms.rdm.api.service;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.util.row.RowMapper;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;

import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Supplier;

public interface VersionFileService {

    /**
     * Создание файла версии нужного типа.
     *
     * @param version        версия справочника
     * @param fileType       тип файла
     * @param versionService сервис для выборки данных из версии
     * @return Путь к сохранённому файлу
     */
    String create(RefBookVersion version, FileType fileType, VersionService versionService);

    /**
     * Генерация файла версии нужного типа с помощью итератора данных.
     * 
     * @param version        версия справочника
     * @param fileType       тип файла
     * @param rowIterator    итератор данных из версии
     * @return Входной поток данных файла
     */
    InputStream generate(RefBookVersion version, FileType fileType, Iterator<Row> rowIterator);

    /**
     * Сохранение файла версии и информации о файле версии.
     *
     * @param version        версия справочника
     * @param fileType       тип файла
     * @param is             входной поток данных файла
     */
    void save(RefBookVersion version, FileType fileType, InputStream is);

    /**
     * Получение поставщика данных файла.
     *
     * @param filePath путь к файлу
     * @return поставщик данных файла
     */
    Supplier<InputStream> supply(String filePath);

    /**
     * Получение данных файла для экспорта.
     *
     * @param version        версия справочника
     * @param fileType       тип файла
     * @param versionService сервис для выборки данных из версии
     * @return Данные файла для экспорта
     */
    ExportFile getFile(RefBookVersion version, FileType fileType, VersionService versionService);

    /**
     * Обработка записей файла.
     *
     * @param fileModel     модель файла
     * @param rowsProcessor обработчик записей
     * @param rowMapper     отображатель записей
     */
    void processRows(FileModel fileModel, RowsProcessor rowsProcessor, RowMapper rowMapper);
}
