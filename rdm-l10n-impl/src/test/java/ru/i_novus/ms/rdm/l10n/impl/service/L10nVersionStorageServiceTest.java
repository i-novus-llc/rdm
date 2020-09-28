package ru.i_novus.ms.rdm.l10n.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.ms.rdm.l10n.api.model.L10nVersionLocale;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeTableRequest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nLocaleInfoService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;
import ru.i_novus.platform.l10n.versioned_data_storage.model.L10nConstants;
import ru.i_novus.platform.l10n.versioned_data_storage.model.L10nLocaleInfo;
import ru.i_novus.platform.versioned_data_storage.pg_impl.dao.StorageConstants;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.l10n.impl.utils.L10nRefBookTestUtils.*;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.toStorageCode;

@RunWith(MockitoJUnitRunner.class)
public class L10nVersionStorageServiceTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final int TEST_OPT_LOCK_VALUE = 10;
    private static final String TEST_REFBOOK_CODE = "L10N_TEST";

    private static final String TEST_LOCALE_CODE = "test";
    private static final L10nLocaleInfo TEST_LOCALE_INFO = new L10nLocaleInfo(TEST_LOCALE_CODE, "Тест", null);

    private static final String TEST_SCHEMA_NAME = L10nConstants.SCHEMA_NAME_PREFIX + TEST_LOCALE_CODE;
    private static final String TEST_STORAGE_NAME = TEST_REFBOOK_CODE + "_storage";
    private static final String DEFAULT_SCHEMA_NAME = StorageConstants.DATA_SCHEMA_NAME;
    private static final String BAD_SCHEMA_NAME = "#bad-schema^name";

    private static final String ATTRIBUTE_ID_CODE = "id";
    private static final String ATTRIBUTE_NAME_CODE = "name";
    private static final String ATTRIBUTE_TEXT_CODE = "text";

    private static final List<L10nLocaleInfo> LOCALE_INFOS = List.of(
            new L10nLocaleInfo("rus", "Русский (по умолчанию)", null),
            new L10nLocaleInfo("eng", "Английский", "English"),
            new L10nLocaleInfo("jap", "Японский", "日本語")
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private L10nVersionStorageServiceImpl versionStorageService;

    @Mock
    private L10nDraftDataService draftDataService;

    @Mock
    private L10nLocaleInfoService localeInfoService;

    @Mock
    private L10nStorageCodeService storageCodeService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private VersionValidation versionValidation;

    @Before
    public void setUp() {

        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testLocalizeTable() {

        RefBookVersionEntity versionEntity = createVersionEntity();
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        String schemaName = TEST_SCHEMA_NAME;
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);
        String testStorageCode = toStorageCode(schemaName, TEST_STORAGE_NAME);
        when(draftDataService.createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName))).thenReturn(testStorageCode);

        LocalizeTableRequest request = new LocalizeTableRequest(versionEntity.getOptLockValue(), TEST_LOCALE_CODE);
        versionStorageService.localizeTable(TEST_REFBOOK_VERSION_ID, request);

        verify(draftDataService).createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName));
        verify(draftDataService).copyAllData(eq(TEST_STORAGE_NAME), eq(testStorageCode));
    }

    @Test
    public void testLocalizeTableFailed() {

        LocalizeTableRequest request = new LocalizeTableRequest(null, null);
        try {
            versionStorageService.localizeTable(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("locale.code.not.found", getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeTableVersionFailed() {

        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.empty());

        LocalizeTableRequest request = new LocalizeTableRequest(null, TEST_LOCALE_CODE);
        try {
            versionStorageService.localizeTable(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(NotFoundException.class));

        } catch (UserException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE, getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeTableVersionStorageFailed() {

        RefBookVersionEntity versionEntity = createVersionEntity();
        versionEntity.setStorageCode(null);
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        LocalizeTableRequest request = new LocalizeTableRequest(null, TEST_LOCALE_CODE);
        try {
            versionStorageService.localizeTable(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("storage.code.not.found", getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeTableSchemaFailed() {

        failLocalizeTableToSchema(null, "locale.code.is.default", UserException.class);
        failLocalizeTableToSchema(DEFAULT_SCHEMA_NAME, "locale.code.is.default", UserException.class);
        failLocalizeTableToSchema(BAD_SCHEMA_NAME, "locale.code.is.invalid", UserException.class);
    }

    private void failLocalizeTableToSchema(String schemaName, String message, Class expectedExceptionClass) {

        RefBookVersionEntity versionEntity = createVersionEntity();
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);

        LocalizeTableRequest request = new LocalizeTableRequest(versionEntity.getOptLockValue(), TEST_LOCALE_CODE);
        try {
            versionStorageService.localizeTable(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(expectedExceptionClass));

        } catch (UserException e) {
            assertEquals(expectedExceptionClass, e.getClass());
            assertEquals(message, getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeData() {

        RefBookVersionEntity versionEntity = createVersionEntity();
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        String schemaName = TEST_SCHEMA_NAME;
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);
        String testStorageCode = toStorageCode(schemaName, TEST_STORAGE_NAME);
        when(draftDataService.createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName))).thenReturn(testStorageCode);

        final int rowCount = 10;
        List<Row> rows = IntStream.range(0, rowCount - 1).mapToObj(this::createRow).collect(toList());

        LocalizeDataRequest request = new LocalizeDataRequest(versionEntity.getOptLockValue(), TEST_LOCALE_CODE, rows);
        versionStorageService.localizeData(TEST_REFBOOK_VERSION_ID, request);

        verify(draftDataService).createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName));
        verify(draftDataService).copyAllData(eq(TEST_STORAGE_NAME), eq(testStorageCode));
        verify(draftDataService).localizeRows(eq(testStorageCode), any());
    }

    @Test
    public void testLocalizeDataWithNullRows() {

        testLocalizeDataWithoutRows(null);
    }

    @Test
    public void testLocalizeDataWithEmptyRows() {

        testLocalizeDataWithoutRows(emptyList());
    }

    @Test
    public void testLocalizeDataWithInvalidRows() {

        final int rowCount = 10;
        List<Row> rows = IntStream.range(0, rowCount - 1).mapToObj(this::createRow).collect(toList());
        rows.forEach(row -> row.setSystemId(null));

        testLocalizeDataWithoutRows(rows);
    }

    private void testLocalizeDataWithoutRows(List<Row> rows) {

        RefBookVersionEntity versionEntity = createVersionEntity();
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        String schemaName = TEST_SCHEMA_NAME;
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);
        String testStorageCode = toStorageCode(schemaName, TEST_STORAGE_NAME);
        when(draftDataService.createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName))).thenReturn(testStorageCode);

        LocalizeDataRequest request = new LocalizeDataRequest(null, TEST_LOCALE_CODE, rows);
        versionStorageService.localizeData(TEST_REFBOOK_VERSION_ID, request);

        int callCount = isEmpty(rows) ? 0 : 1;
        verify(draftDataService, times(callCount)).createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName));
        verify(draftDataService, times(callCount)).copyAllData(eq(TEST_STORAGE_NAME), eq(testStorageCode));
        verify(draftDataService, times(0)).updateRows(eq(testStorageCode), any());
    }

    @Test
    public void testLocalizeDataFailed() {

        LocalizeDataRequest request = new LocalizeDataRequest(null, null, null);
        try {
            versionStorageService.localizeData(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("locale.code.not.found", getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeTableRequest() {

        LocalizeTableRequest request = new LocalizeTableRequest(TEST_OPT_LOCK_VALUE, TEST_LOCALE_CODE);

        LocalizeTableRequest sameRequest = new LocalizeTableRequest();
        sameRequest.setOptLockValue(TEST_OPT_LOCK_VALUE);
        sameRequest.setLocaleCode(TEST_LOCALE_CODE);

        assertEquals(request.toString(), sameRequest.toString());
        assertEquals(request.getOptLockValue(), sameRequest.getOptLockValue());
        assertEquals(request.getLocaleCode(), sameRequest.getLocaleCode());
    }

    @Test
    public void testLocalizeDataRequest() {

        final int rowCount = 10;
        List<Row> rows = IntStream.range(0, rowCount - 1).mapToObj(this::createRow).collect(toList());

        LocalizeDataRequest request = new LocalizeDataRequest(TEST_OPT_LOCK_VALUE, TEST_LOCALE_CODE, rows);

        LocalizeDataRequest sameRequest = new LocalizeDataRequest();
        sameRequest.setOptLockValue(TEST_OPT_LOCK_VALUE);
        sameRequest.setLocaleCode(TEST_LOCALE_CODE);
        sameRequest.setRows(rows);

        assertEquals(request.toString(), sameRequest.toString());
        assertEquals(request.getOptLockValue(), sameRequest.getOptLockValue());
        assertEquals(request.getLocaleCode(), sameRequest.getLocaleCode());
        assertEquals(request.getRows(), sameRequest.getRows());
    }

    @Test
    public void testSearchVersionLocales() {

        when(localeInfoService.search(any())).thenReturn(LOCALE_INFOS);

        List<String> localeCodes = LOCALE_INFOS.stream().map(L10nLocaleInfo::getCode).collect(toList());
        Map<String, String> localeSchemas = localeCodes.stream().collect(toMap(identity(), this::toSchemaName));
        when(storageCodeService.toSchemaNames(eq(localeCodes))).thenReturn(localeSchemas);

        List<String> existentSchemaNames = localeSchemas.entrySet().stream()
                .filter(e -> !localeCodes.get(localeCodes.size() - 1).equals(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(toList());
        when(draftDataService.getExistentSchemaNames(any())).thenReturn(existentSchemaNames);

        List<L10nLocaleInfo> expectedLocaleInfos = LOCALE_INFOS.stream()
                .filter(info -> existentSchemaNames.contains(toSchemaName(info.getCode())))
                .collect(toList());
        List<L10nVersionLocale> actualVersionLocales = versionStorageService
                .searchVersionLocales(TEST_REFBOOK_VERSION_ID).getContent();
        assertEquals(expectedLocaleInfos.size(), actualVersionLocales.size());

        IntStream.range(0, expectedLocaleInfos.size()).forEach(index ->
                assertLocales(expectedLocaleInfos.get(index), actualVersionLocales.get(index))
        );
    }

    @Test
    public void testGetVersionLocale() {

        when(localeInfoService.find(eq(TEST_LOCALE_CODE))).thenReturn(TEST_LOCALE_INFO);
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(TEST_SCHEMA_NAME);

        List<String> existentSchemaNames = List.of(TEST_SCHEMA_NAME);
        when(draftDataService.getExistentSchemaNames(any())).thenReturn(existentSchemaNames);

        L10nVersionLocale actualVersionLocale = versionStorageService.getVersionLocale(TEST_REFBOOK_VERSION_ID, TEST_LOCALE_CODE);
        assertLocales(TEST_LOCALE_INFO, actualVersionLocale);
    }

    private String toSchemaName(String localeCode) {
        return L10nConstants.SCHEMA_NAME_PREFIX + localeCode;
    }

    private void assertLocales(L10nLocaleInfo localeInfo, L10nVersionLocale versionLocale) {

        assertObjects(Assert::assertNotEquals, localeInfo, versionLocale);
        assertEquals(Integer.valueOf(TEST_REFBOOK_VERSION_ID), versionLocale.getVersionId());
        assertEquals(localeInfo.getCode(), versionLocale.getLocaleCode());
        assertEquals(localeInfo.getName(), versionLocale.getLocaleName());
        assertEquals(localeInfo.getSelfName(), versionLocale.getLocaleSelfName());
    }

    @Test
    public void testGetLocaleStorageCode() {

        String testStorageCode = toStorageCode(TEST_SCHEMA_NAME, TEST_STORAGE_NAME);
        when(storageCodeService.toStorageCode(eq(TEST_STORAGE_NAME), eq(TEST_LOCALE_CODE))).thenReturn(testStorageCode);

        String localeStorageCode = versionStorageService.getLocaleStorageCode(TEST_STORAGE_NAME, TEST_LOCALE_CODE);
        assertEquals(testStorageCode, localeStorageCode);
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setStorageCode(TEST_STORAGE_NAME);

        Structure structure = createStructure();
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    private Structure createStructure() {

        return new Structure(asList(
                Structure.Attribute.buildPrimary(ATTRIBUTE_ID_CODE, "Идентификатор", FieldType.INTEGER, null),
                Structure.Attribute.build(ATTRIBUTE_NAME_CODE, "Наименование", FieldType.STRING, null),
                Structure.Attribute.build(ATTRIBUTE_TEXT_CODE, "Текст", FieldType.STRING, null)
        ), null);
    }

    private Row createRow(int index) {

        Map<String, Object> map = new HashMap<>(3);
        map.put(ATTRIBUTE_ID_CODE, BigInteger.valueOf(index));
        map.put(ATTRIBUTE_NAME_CODE, "name_" + index);
        map.put(ATTRIBUTE_TEXT_CODE, "text_" + index);

        return new Row((long) index, map);
    }
}