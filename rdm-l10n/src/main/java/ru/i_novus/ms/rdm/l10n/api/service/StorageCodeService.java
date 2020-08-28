package ru.i_novus.ms.rdm.l10n.api.service;

import ru.i_novus.ms.rdm.l10n.api.model.criteria.StorageCodeCriteria;

import java.util.List;
import java.util.Map;

/**
 * Сервис формирования кода хранилища.
 */
public interface StorageCodeService {

    /**
     * Определение кода хранилища по критерию.
     *
     * @param criteria критерий определения хранилища
     * @return Код хранилища
     */
    String toStorageCode(StorageCodeCriteria criteria);

    /**
     * Определение наименования схемы по критерию.
     *
     * @param criteria критерий определения хранилища
     * @return Наименование схемы
     */
    String getSchemaName(StorageCodeCriteria criteria);

    /**
     * Преобразование кода локали в наименование схемы.
     *
     * <p>
     * Наименование схемы должно содержать только
     * строчные латинские буквы a-z, цифры 0-9 и символ подчёркивания "_".
     *
     * <p>
     * В postgres максимальная длина имени = NAMEDATALEN - 1 = 64 - 1,
     * поэтому длина наименования схемы должна быть <= 64 - 1 - L10N_SCHEMA_NAME_PREFIX.len() .
     * Это ограничение проверяется с помощью
     * {@link ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils#isValidSchemaName}.
     *
     * @param localeCode код локали
     * @return Наименование схемы
     */
    String toSchemaName(String localeCode);

    /**
     * Преобразование кодов локалей в наименования схем.
     *
     * @param localeCodes список кодов локали
     * @return Набор кодов локалей с наименованиями схем
     */
    Map<String, String> toLocaleSchemas(List<String> localeCodes);
}
