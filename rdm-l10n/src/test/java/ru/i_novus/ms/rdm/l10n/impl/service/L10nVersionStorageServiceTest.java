package ru.i_novus.ms.rdm.l10n.impl.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.toStorageCode;

@RunWith(MockitoJUnitRunner.class)
public class L10nVersionStorageServiceTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final String TEST_REFBOOK_CODE = "L10N_TEST";
    private static final String TEST_LOCALE_CODE = "test";
    private static final String TEST_SCHEMA_NAME = "l10n_" + TEST_LOCALE_CODE;
    private static final String TEST_STORAGE_NAME = TEST_REFBOOK_CODE + "_storage";

    private static final String ATTRIBUTE_ID_CODE = "id";
    private static final String ATTRIBUTE_NAME_CODE = "name";
    private static final String ATTRIBUTE_TEXT_CODE = "text";

    @InjectMocks
    private L10nVersionStorageServiceImpl versionStorageService;

    @Mock
    private L10nDraftDataService draftDataService;

    @Mock
    private L10nStorageCodeService storageCodeService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testLocalizeData() {

        Structure structure = createStructure();
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setStructure(structure);
        versionEntity.setStorageCode(TEST_STORAGE_NAME);

        when(storageCodeService.toSchemaName(eq(TEST_LOCALE_CODE))).thenReturn(TEST_SCHEMA_NAME);
        when(versionRepository.findById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(Optional.of(versionEntity));

        String testStorageCode = toStorageCode(TEST_SCHEMA_NAME, TEST_STORAGE_NAME);
        when(draftDataService.createLocalizedTable(eq(TEST_STORAGE_NAME), eq(TEST_SCHEMA_NAME))).thenReturn(testStorageCode);

        final int rowCount = 10;
        List<Row> rows = IntStream.range(0, rowCount - 1).mapToObj(this::createRow).collect(toList());

        LocalizeDataRequest request = new LocalizeDataRequest(null, TEST_LOCALE_CODE, rows);
        versionStorageService.localizeData(TEST_REFBOOK_VERSION_ID, request);

        verify(draftDataService).createLocalizedTable(eq(TEST_STORAGE_NAME), eq(TEST_SCHEMA_NAME));
        verify(draftDataService).copyAllData(eq(TEST_STORAGE_NAME), eq(testStorageCode));
        verify(draftDataService).updateRows(eq(TEST_SCHEMA_NAME), any());
    }

    @Test
    public void testGetLocaleStorageCode() {

        String testStorageCode = toStorageCode(TEST_SCHEMA_NAME, TEST_STORAGE_NAME);
        when(storageCodeService.toStorageCode(eq(TEST_STORAGE_NAME), eq(TEST_LOCALE_CODE))).thenReturn(testStorageCode);

        String localeStorageCode = versionStorageService.getLocaleStorageCode(TEST_STORAGE_NAME, TEST_LOCALE_CODE);
        assertEquals(testStorageCode, localeStorageCode);
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