package ru.i_novus.ms.rdm.l10n.impl.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class L10nVersionServiceTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final String TEST_REFBOOK_CODE = "L10N_TEST";
    private static final String TEST_LOCALE_CODE = "test";
    private static final String TEST_SCHEMA_NAME = "l10n_" + TEST_LOCALE_CODE;
    private static final String TEST_STORAGE_NAME = TEST_REFBOOK_CODE + "_storage";

    private static final String ATTRIBUTE_ID_CODE = "id";
    private static final String ATTRIBUTE_NAME_CODE = "name";
    private static final String ATTRIBUTE_TEXT_CODE = "text";

    @InjectMocks
    private L10nVersionServiceImpl l10nVersionService;

    @Mock
    private L10nDraftDataService draftDataService;

    @Mock
    private L10nStorageCodeService storageCodeService;

    @Mock
    private VersionService versionService;

    @Test
    public void testLocalizeData() {

        when(storageCodeService.toLocaleSchema(eq(TEST_LOCALE_CODE))).thenReturn(TEST_SCHEMA_NAME);
        when(draftDataService.schemaExists(eq(TEST_SCHEMA_NAME))).thenReturn(true);
        when(versionService.getStorageCode(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(TEST_STORAGE_NAME);

        Structure structure = createStructure();
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(structure);

        final int rowCount = 10;
        List<Row> rows = IntStream.range(0, rowCount - 1).mapToObj(this::createRow).collect(toList());

        LocalizeDataRequest request = new LocalizeDataRequest(null, TEST_LOCALE_CODE, rows);
        l10nVersionService.localizeData(TEST_REFBOOK_VERSION_ID, request);

        verify(draftDataService).localizeTable(eq(TEST_STORAGE_NAME), eq(TEST_SCHEMA_NAME));
        verify(draftDataService).updateRows(eq(TEST_SCHEMA_NAME), any());
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