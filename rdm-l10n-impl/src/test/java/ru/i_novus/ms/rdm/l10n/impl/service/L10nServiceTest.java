package ru.i_novus.ms.rdm.l10n.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.UserException;
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
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeTableRequest;
import ru.i_novus.ms.rdm.l10n.impl.BaseTest;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;
import ru.i_novus.platform.l10n.versioned_data_storage.pg_impl.dao.L10nConstants;
import ru.i_novus.platform.versioned_data_storage.pg_impl.dao.StorageConstants;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.l10n.impl.utils.StructureTestConstants.*;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.toStorageCode;

@RunWith(MockitoJUnitRunner.class)
public class L10nServiceTest extends BaseTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final int TEST_OPT_LOCK_VALUE = 10;
    private static final String TEST_REFBOOK_CODE = "L10N_TEST";
    private static final int TEST_ROW_COUNT = 10;

    private static final String TEST_LOCALE_CODE = "test";

    private static final String TEST_SCHEMA_NAME = L10nConstants.SCHEMA_NAME_PREFIX + TEST_LOCALE_CODE;
    private static final String TEST_STORAGE_NAME = TEST_REFBOOK_CODE + "_storage";
    private static final String DEFAULT_SCHEMA_NAME = StorageConstants.DATA_SCHEMA_NAME;
    private static final String BAD_SCHEMA_NAME = "#bad-schema^name";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private L10nServiceImpl l10nService;

    @Mock
    private L10nDraftDataService draftDataService;

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
    public void testLocalizeData() {

        testLocalizeData(createStructure());
    }

    @Test
    public void testLocalizeDataWithoutReferences() {

        Structure structure = createStructure();
        structure.getAttributes().removeIf(Structure.Attribute::isReferenceType);
        structure = new Structure(structure.getAttributes(), null);

        testLocalizeData(structure);
    }

    @Test
    public void testLocalizeDataWithAllLocalizables() {

        Structure structure = createStructure();
        structure.getAttributes().forEach(attribute -> attribute.setLocalizable(Boolean.TRUE));

        testLocalizeData(structure);
    }

    @Test
    public void testLocalizeDataWithLocalizableReference() {

        Structure structure = createStructure();
        structure.getAttribute(REFERENCE_ATTRIBUTE_CODE).setLocalizable(Boolean.TRUE);

        testLocalizeData(structure);
    }

    private void testLocalizeData(Structure structure) {

        RefBookVersionEntity versionEntity = createVersionEntity(structure);
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        String schemaName = TEST_SCHEMA_NAME;
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);
        String testStorageCode = toStorageCode(schemaName, TEST_STORAGE_NAME);
        when(draftDataService.storageExists(eq(testStorageCode))).thenReturn(false);
        when(draftDataService.createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName))).thenReturn(testStorageCode);

        LocalizeDataRequest request = new LocalizeDataRequest(versionEntity.getOptLockValue(), TEST_LOCALE_CODE, createRows());
        l10nService.localizeData(TEST_REFBOOK_VERSION_ID, request);

        verify(draftDataService).createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName));
        verify(draftDataService).copyAllData(eq(TEST_STORAGE_NAME), eq(testStorageCode));
        verify(draftDataService).localizeRows(eq(testStorageCode), any());
    }

    @Test
    public void testLocalizeDataWhenStorageExists() {

        RefBookVersionEntity versionEntity = createVersionEntity(createStructure());
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        String schemaName = TEST_SCHEMA_NAME;
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);
        String testStorageCode = toStorageCode(schemaName, TEST_STORAGE_NAME);
        when(draftDataService.storageExists(eq(testStorageCode))).thenReturn(true);

        LocalizeDataRequest request = new LocalizeDataRequest(versionEntity.getOptLockValue(), TEST_LOCALE_CODE, createRows());
        l10nService.localizeData(TEST_REFBOOK_VERSION_ID, request);

        verify(draftDataService, times(0)).createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName));
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

        List<Row> rows = createRows();
        rows.forEach(row -> row.setSystemId(null));

        testLocalizeDataWithoutRows(rows);
    }

    private void testLocalizeDataWithoutRows(List<Row> rows) {

        RefBookVersionEntity versionEntity = createVersionEntity(createStructure());
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        String schemaName = TEST_SCHEMA_NAME;
        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);
        String testStorageCode = toStorageCode(schemaName, TEST_STORAGE_NAME);
        when(draftDataService.createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName))).thenReturn(testStorageCode);

        LocalizeDataRequest request = new LocalizeDataRequest(null, TEST_LOCALE_CODE, rows);
        l10nService.localizeData(TEST_REFBOOK_VERSION_ID, request);

        int callCount = isEmpty(rows) ? 0 : 1;
        verify(draftDataService, times(callCount)).createLocalizedTable(eq(TEST_STORAGE_NAME), eq(schemaName));
        verify(draftDataService, times(callCount)).copyAllData(eq(TEST_STORAGE_NAME), eq(testStorageCode));
        verify(draftDataService, times(0)).updateRows(eq(testStorageCode), any());
    }

    @Test
    public void testLocalizeDataFailed() {

        LocalizeDataRequest request = new LocalizeDataRequest(null, null, null);
        try {
            l10nService.localizeData(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("locale.code.not.found", getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeTableVersionFailed() {

        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.empty());

        List<Row> rows = List.of(createRow(0));
        LocalizeDataRequest request = new LocalizeDataRequest(null, TEST_LOCALE_CODE, rows);
        try {
            l10nService.localizeData(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(NotFoundException.class));

        } catch (UserException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE, getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeDataVersionStorageFailed() {

        RefBookVersionEntity versionEntity = createVersionEntity(createStructure());
        versionEntity.setStorageCode(null);
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        List<Row> rows = List.of(createRow(0));
        LocalizeDataRequest request = new LocalizeDataRequest(null, TEST_LOCALE_CODE, rows);
        try {
            l10nService.localizeData(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("storage.code.not.found", getExceptionMessage(e));
        }
    }

    @Test
    public void testLocalizeDataSchemaFailed() {

        failLocalizeDataToSchema(null, "locale.code.is.default", UserException.class);
        failLocalizeDataToSchema(DEFAULT_SCHEMA_NAME, "locale.code.is.default", UserException.class);
        failLocalizeDataToSchema(BAD_SCHEMA_NAME, "locale.code.is.invalid", UserException.class);
    }

    @SuppressWarnings("SameParameterValue")
    private void failLocalizeDataToSchema(String schemaName, String message, Class expectedExceptionClass) {

        RefBookVersionEntity versionEntity = createVersionEntity(createStructure());
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(schemaName);

        List<Row> rows = List.of(createRow(0));
        LocalizeDataRequest request = new LocalizeDataRequest(versionEntity.getOptLockValue(), TEST_LOCALE_CODE, rows);
        try {
            l10nService.localizeData(TEST_REFBOOK_VERSION_ID, request);
            fail(getFailedMessage(expectedExceptionClass));

        } catch (UserException e) {
            assertEquals(expectedExceptionClass, e.getClass());
            assertEquals(message, getExceptionMessage(e));
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

    private RefBookVersionEntity createVersionEntity(Structure structure) {

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();

        versionEntity.setStorageCode(TEST_STORAGE_NAME);
        versionEntity.setStructure(structure);

        return versionEntity;
    }

    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }

    private List<Row> createRows() {

        return IntStream.range(0, TEST_ROW_COUNT - 1).mapToObj(this::createRow).collect(toList());
    }

    private Row createRow(int index) {

        Map<String, Object> map = new HashMap<>(3);
        map.put(ID_ATTRIBUTE_CODE, BigInteger.valueOf(index));
        map.put(NAME_ATTRIBUTE_CODE, "name_" + index);
        map.put(STRING_ATTRIBUTE_CODE, "text_" + index);
        map.put(INTEGER_ATTRIBUTE_CODE, BigInteger.valueOf(index));
        map.put(FLOAT_ATTRIBUTE_CODE, BigDecimal.valueOf(index + index * 0.1));
        map.put(BOOLEAN_ATTRIBUTE_CODE, index > TEST_ROW_COUNT / 2);
        map.put(DATE_ATTRIBUTE_CODE, LocalDate.now());
        map.put(REFERENCE_ATTRIBUTE_CODE, new Reference("value_" + index, "display_" + index));

        return new Row((long) index, map);
    }
}