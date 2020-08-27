package ru.i_novus.ms.rdm.l10n.impl.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.l10n.api.model.criteria.StorageCodeCriteria;
import ru.i_novus.platform.l10n.versioned_data_storage.model.L10nConstants;
import ru.i_novus.platform.versioned_data_storage.pg_impl.dao.StorageConstants;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.generateStorageName;

@RunWith(MockitoJUnitRunner.class)
public class StorageCodeServiceTest {

    private static final Map<String, Map<String, String>> L10N_SCHEMA_NAME_SUFFIXES = getL10nSchemaNameSuffixes();

    @InjectMocks
    private StorageCodeServiceImpl storageCodeService;

    private static Map<String, Map<String, String>> getL10nSchemaNameSuffixes() {

        Map<String, Map<String, String>> languageMap = new HashMap<>();

        String language = "ru";
        Map<String, String> countryMap = new HashMap<>();
        countryMap.put("", language);
        addCountry(countryMap, language, "RU");
        languageMap.put(language, countryMap);

        language = "en";
        countryMap = new HashMap<>();
        countryMap.put("", language);
        addCountry(countryMap, language, "GB");
        addCountry(countryMap, language, "US");
        languageMap.put(language, countryMap);

        language = "zh";
        countryMap = new HashMap<>();
        countryMap.put("", language);
        addCountry(countryMap, language, "CN");
        addCountry(countryMap, language, "HK");
        languageMap.put(language, countryMap);

        language = "ja";
        countryMap = new HashMap<>();
        countryMap.put("", language);
        addCountry(countryMap, language, "JP");
        countryMap.put("JP_JP_#", language + "_jp_jp_0");
        languageMap.put(language, countryMap);

        return languageMap;
    }

    private static void addCountry(Map<String, String> countryMap, String language, String country) {
        countryMap.put(country, language + "_" + country.toLowerCase());
    }

    @Test
    public void testToStorageCode() {

        String sourceCode = "12345678-1234-5678-90ab-cdef56789090";

        Locale locale = new Locale("");
        StorageCodeCriteria criteria = new StorageCodeCriteria(sourceCode, locale.toString());
        String targetCode = storageCodeService.toStorageCode(criteria);
        assertEquals(sourceCode, targetCode);

        L10N_SCHEMA_NAME_SUFFIXES.forEach((language, countries) ->
                countries.forEach((country, suffix) -> {
                    String expectedCode = L10nConstants.SCHEMA_NAME_PREFIX + suffix +
                            StorageConstants.CODE_SEPARATOR + sourceCode;
                    testToStorageCode(sourceCode, language, country, expectedCode);
                })
        );
    }

    private void testToStorageCode(String sourceCode, String language, String country, String expectedCode) {

        Locale locale = new Locale(language, country);
        StorageCodeCriteria criteria = new StorageCodeCriteria(sourceCode, locale.toString());

        String actualCode = storageCodeService.toStorageCode(criteria);
        assertEquals(expectedCode, actualCode);
    }

    @Test
    public void testGenerateStorageName() {

        String firstName = generateStorageName();
        assertNotNull(firstName);

        String secondName = generateStorageName();
        assertNotNull(firstName);
        assertNotEquals(firstName, secondName);
    }
}
