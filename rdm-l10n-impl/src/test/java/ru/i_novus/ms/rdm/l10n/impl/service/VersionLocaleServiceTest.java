package ru.i_novus.ms.rdm.l10n.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.l10n.api.model.L10nVersionLocale;
import ru.i_novus.ms.rdm.l10n.impl.BaseTest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nLocaleInfoService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;
import ru.i_novus.platform.l10n.versioned_data_storage.model.L10nLocaleInfo;
import ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionLocaleServiceTest extends BaseTest {

    public static final String SCHEMA_NAME_PREFIX = "l10n_"; // from l10n-vds::L10nConstants

    private static final String TEST_REFBOOK_CODE = "test";
    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final String TEST_VERSION_STORAGE_CODE = "test-storage-code";

    private static final String TEST_LOCALE_CODE = "test";
    private static final String TEST_LOCALE_NAME = "Тест";
    private static final L10nLocaleInfo TEST_LOCALE_INFO = new L10nLocaleInfo(TEST_LOCALE_CODE, TEST_LOCALE_NAME, null);

    private static final String TEST_SCHEMA_NAME = SCHEMA_NAME_PREFIX + TEST_LOCALE_CODE;

    private static final List<L10nLocaleInfo> LOCALE_INFOS = List.of(
            new L10nLocaleInfo("rus", "Русский (по умолчанию)", null),
            new L10nLocaleInfo("eng", "Английский", "English"),
            new L10nLocaleInfo("jap", "Японский", "日本語")
    );

    @InjectMocks
    private VersionLocaleServiceImpl versionLocaleService;

    @Mock
    private L10nLocaleInfoService localeInfoService;

    @Mock
    private L10nStorageCodeService storageCodeService;

    @Mock
    private L10nDraftDataService draftDataService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testFindRefBookLocales() {

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(TEST_REFBOOK_VERSION_ID);
        versionEntity.setStorageCode(TEST_VERSION_STORAGE_CODE);

        when(versionRepository
                .findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                        eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(versionEntity);

        List<String> expected = List.of(TEST_LOCALE_CODE);

        when(draftDataService.findTableLocaleCodes(eq(TEST_VERSION_STORAGE_CODE))).thenReturn(expected);

        List<String> actual = versionLocaleService.findRefBookLocales(TEST_REFBOOK_CODE);
        assertListEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("java:S5778")
    public void testFindRefBookLocalesVersionFailed() {

        when(versionRepository
                .findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                        eq(TEST_REFBOOK_CODE), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(null);

        try {
            versionLocaleService.findRefBookLocales(TEST_REFBOOK_CODE);
            fail(getFailedMessage(NotFoundException.class));

        } catch (UserException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }

    @Test
    public void testSearchVersionLocales() {

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(TEST_REFBOOK_VERSION_ID);
        versionEntity.setStructure(createVersionStructure());

        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        when(localeInfoService.search(any())).thenReturn(LOCALE_INFOS);

        List<String> localeCodes = LOCALE_INFOS.stream().map(L10nLocaleInfo::getCode).collect(toList());
        Map<String, String> localeSchemas = localeCodes.stream().collect(toMap(identity(), this::toSchemaName));
        localeSchemas.put(LOCALE_INFOS.get(0).getCode(), "data");
        when(storageCodeService.toSchemaNames(eq(localeCodes))).thenReturn(localeSchemas);

        List<L10nLocaleInfo> expectedLocaleInfos = LOCALE_INFOS.stream()
                .filter(info -> !StorageUtils.isDefaultSchema(localeSchemas.get(info.getCode())))
                .collect(toList());
        List<L10nVersionLocale> actualVersionLocales = versionLocaleService
                .searchVersionLocales(TEST_REFBOOK_VERSION_ID).getContent();
        assertEquals(expectedLocaleInfos.size(), actualVersionLocales.size());

        IntStream.range(0, expectedLocaleInfos.size()).forEach(index -> {

            L10nVersionLocale actual = actualVersionLocales.get(index);
            L10nLocaleInfo expected = expectedLocaleInfos.stream()
                    .filter(info -> actual.getLocaleCode().equals(info.getCode()))
                    .findFirst().orElse(null);
            assertLocales(expected, actual);
        });
    }

    @Test
    public void testSearchVersionLocalesVersionFailed() {

        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.empty());

        List<L10nVersionLocale> actualVersionLocales = versionLocaleService
                .searchVersionLocales(TEST_REFBOOK_VERSION_ID).getContent();
        assertEquals(0, actualVersionLocales.size());

        verify(localeInfoService, times(0)).search(any());
    }

    @Test
    public void testGetVersionLocale() {

        when(localeInfoService.find(eq(TEST_LOCALE_CODE))).thenReturn(TEST_LOCALE_INFO);
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(TEST_SCHEMA_NAME);

        L10nVersionLocale actualVersionLocale = versionLocaleService.getVersionLocale(TEST_REFBOOK_VERSION_ID, TEST_LOCALE_CODE);
        assertLocales(TEST_LOCALE_INFO, actualVersionLocale);
    }

    @Test
    public void testGetVersionLocaleOnNull() {

        when(localeInfoService.find(eq(TEST_LOCALE_CODE))).thenReturn(null);
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(TEST_SCHEMA_NAME);

        L10nVersionLocale actualVersionLocale = versionLocaleService.getVersionLocale(TEST_REFBOOK_VERSION_ID, TEST_LOCALE_CODE);
        assertNull(actualVersionLocale);
    }

    @Test
    @SuppressWarnings("java:S5778")
    public void testGetVersionLocaleOnDefaultFailed() {

        when(localeInfoService.find(eq(TEST_LOCALE_CODE))).thenReturn(TEST_LOCALE_INFO);
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(null);

        try {
            versionLocaleService.getVersionLocale(TEST_REFBOOK_VERSION_ID, TEST_LOCALE_CODE);
            fail(getFailedMessage(UserException.class));

        } catch (UserException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("locale.code.is.default", getExceptionMessage(e));
        }
    }

    @Test
    public void testGetLocaleName() {

        when(localeInfoService.find(isNull())).thenReturn(null);

        String localeName = versionLocaleService.getLocaleName(null);
        assertNull(localeName);

        when(localeInfoService.find(eq(TEST_LOCALE_CODE))).thenReturn(TEST_LOCALE_INFO);

        localeName = versionLocaleService.getLocaleName(TEST_LOCALE_CODE);
        assertEquals(TEST_LOCALE_NAME, localeName);
    }

    private String toSchemaName(String localeCode) {
        return SCHEMA_NAME_PREFIX + localeCode;
    }

    private void assertLocales(L10nLocaleInfo localeInfo, L10nVersionLocale versionLocale) {

        assertObjects(Assert::assertNotEquals, localeInfo, versionLocale);
        assertEquals(Integer.valueOf(TEST_REFBOOK_VERSION_ID), versionLocale.getVersionId());
        assertEquals(localeInfo.getCode(), versionLocale.getLocaleCode());
        assertEquals(localeInfo.getName(), versionLocale.getLocaleName());
        assertEquals(localeInfo.getSelfName(), versionLocale.getLocaleSelfName());
    }

    private Structure createVersionStructure() {

        Structure.Attribute idAttribute = Structure.Attribute.buildPrimary("id", "id", FieldType.INTEGER, null);
        Structure.Attribute localeAttribute = Structure.Attribute.buildLocalizable("name", "name", FieldType.INTEGER, null);

        return new Structure(Arrays.asList(idAttribute, localeAttribute), null);
    }
}