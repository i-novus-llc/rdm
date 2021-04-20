package ru.i_novus.ms.rdm.n2o.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.n2o.BaseTest;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;
import ru.i_novus.ms.rdm.n2o.model.FormAttribute;
import ru.i_novus.ms.rdm.n2o.model.UiPassport;
import ru.i_novus.ms.rdm.n2o.strategy.BaseUiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.draft.*;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.*;

@RunWith(MockitoJUnitRunner.class)
public class CreateDraftControllerTest extends BaseTest {

    private static final int TEST_REFBOOK_ID = -10;
    private static final String TEST_REFBOOK_CODE = "test";
    private static final String TEST_REFBOOK_NAME = "Тест";

    private static final int TEST_REFBOOK_VERSION_ID = -100;
    private static final int TEST_OPT_LOCK_VALUE = 10;

    private static final int TEST_REFBOOK_DRAFT_ID = -101;
    private static final int TEST_DRAFT_OPT_LOCK_VALUE = 11;

    private static final long TEST_SYSTEM_ID = 51;
    private static final String TEST_ROW_VALUE_HASH = "HASH";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CreateDraftController controller;

    @Mock
    private RefBookService refBookService;
    @Mock
    private VersionRestService versionService;
    @Mock
    private DraftRestService draftService;

    @Mock
    private StructureController structureController;
    @Mock
    private DataRecordController dataRecordController;

    @Mock
    private DefaultFindOrCreateDraftStrategy defaultFindOrCreateDraftStrategy;
    @Mock
    private DefaultValidateIsDraftStrategy defaultValidateIsDraftStrategy;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() throws NoSuchFieldException {
        JsonUtil.jsonMapper = objectMapper;

        final UiStrategyLocator strategyLocator = new BaseUiStrategyLocator(getStrategies());
        FieldSetter.setField(controller, CreateDraftController.class.getDeclaredField("strategyLocator"), strategyLocator);
    }

    @Test
    public void testEditPassport() {

        testEditPassport(createPassport());
    }

    @Test
    public void testEditPassportNull() {

        testEditPassport(null);
    }

    private void testEditPassport(UiPassport passport) {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.editPassport(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, passport);
        assertEquals(uiDraft, actual);

        ArgumentCaptor<RefBookUpdateRequest> captor = ArgumentCaptor.forClass(RefBookUpdateRequest.class);
        verify(refBookService, times(1)).update(captor.capture());

        RefBookUpdateRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_REFBOOK_VERSION_ID), request.getVersionId());
        assertEquals(Integer.valueOf(TEST_OPT_LOCK_VALUE), request.getOptLockValue());

        if (passport != null) {
            assertEquals(TEST_REFBOOK_CODE, request.getCode());
            assertEquals(3, request.getPassport().size());
        } else {
            assertNull(request.getCode());
            assertNull(request.getPassport());
        }
    }

    @Test
    public void testEditPassportWhenVersion() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.editPassport(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createPassport());
        assertEquals(uiDraft, actual);
    }

    @Test
    public void testEditPassportWhenCreateDraft() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.editPassport(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createPassport());
        assertEquals(uiDraft, actual);
    }

    @Test
    public void testCreateAttribute() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.createAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .createAttribute(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testCreateAttributeWhenVersion() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.createAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .createAttribute(eq(TEST_REFBOOK_DRAFT_ID), eq(TEST_DRAFT_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testUpdateAttribute() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.updateAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .updateAttribute(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testUpdateAttributeWhenVersion() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.updateAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .updateAttribute(eq(TEST_REFBOOK_DRAFT_ID), eq(TEST_DRAFT_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testDeleteAttribute() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.deleteAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, NAME_ATTRIBUTE_CODE);
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .deleteAttribute(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), eq(NAME_ATTRIBUTE_CODE));
    }

    @Test
    public void testDeleteAttributeWhenVersion() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.deleteAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, NAME_ATTRIBUTE_CODE);
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .deleteAttribute(eq(TEST_REFBOOK_DRAFT_ID), eq(TEST_DRAFT_OPT_LOCK_VALUE), eq(NAME_ATTRIBUTE_CODE));
    }

    @Test
    public void testCreateDataRecord() {

        testCreateDataRecord(null);
    }

    @Test
    public void testCreateDataRecordWhenEmpty() {

        testCreateDataRecord(new RefBookRowValue());
    }

    private void testCreateDataRecord(RefBookRowValue rowValue) {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        Row row = createRow(null);
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version.getStructure());

        if (rowValue != null) {
            SearchDataCriteria criteria = new SearchDataCriteria();
            criteria.setPageSize(1);
            when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                    .thenReturn(new PageImpl<>(emptyList(), criteria, 1));
        }

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
        assertEquals(uiDraft, actual);

        verify(dataRecordController, times(1))
                .updateData(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(Row.class));
    }

    @Test
    @SuppressWarnings("java:S5778")
    public void testCreateDataRecordFailed() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft( version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        Row row = createRow(null);
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version.getStructure());

        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(createRowValue()), new SearchDataCriteria(), 1));

        try {
            controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
            fail(getFailedMessage(UserException.class));

        } catch (RuntimeException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("data.row.pk.exists", getExceptionMessage(e));
        }
    }

    @Test
    public void testUpdateDataRecord() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        Row row = createRow(1L);
        UiDraft actual = controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
        assertEquals(uiDraft, actual);

        verify(dataRecordController, times(1))
                .updateData(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(Row.class));
    }

    @Test
    public void testUpdateDataRecordWhenVersion() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        RefBookRowValue oldRowValue = createRowValue();
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(oldRowValue), new SearchDataCriteria(), 1));

        RefBookRowValue newRowValue = createRowValue();
        newRowValue.setSystemId(oldRowValue.getSystemId() + 1);
        when(versionService.search(eq(TEST_REFBOOK_DRAFT_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(newRowValue), new SearchDataCriteria(), 1));

        Row row = createRow(1L);
        UiDraft actual = controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
        assertEquals(uiDraft, actual);

        verify(dataRecordController, times(1))
                .updateData(eq(TEST_REFBOOK_DRAFT_ID), eq(TEST_DRAFT_OPT_LOCK_VALUE), any(Row.class));
    }

    @Test
    public void testUpdateDataRecordFailed() {

        testUpdateDataRecordFailed(null);
        testUpdateDataRecordFailed(new Row());
        testUpdateDataRecordFailed(new Row(1L, emptyMap()));
        testUpdateDataRecordFailed(createEmptyRow(1L));
    }

    @SuppressWarnings("java:S5778")
    private void testUpdateDataRecordFailed(Row row) {

        try {
            controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
            fail(getFailedMessage(UserException.class));

        } catch (RuntimeException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("data.row.is.empty", getExceptionMessage(e));
        }
    }

    @Test
    public void testDeleteDataRecord() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.deleteDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, TEST_SYSTEM_ID);
        assertEquals(uiDraft, actual);

        ArgumentCaptor<DeleteDataRequest> captor = ArgumentCaptor.forClass(DeleteDataRequest.class);
        verify(draftService, times(1))
                .deleteData(eq(TEST_REFBOOK_VERSION_ID), captor.capture());

        DeleteDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_OPT_LOCK_VALUE), request.getOptLockValue());

        List<Row> rows = request.getRows();
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Row row = rows.get(0);
        assertNotNull(row);
        assertEquals(Long.valueOf(TEST_SYSTEM_ID), row.getSystemId());
    }

    @Test
    public void testDeleteDataRecordWhenVersion() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        RefBookRowValue oldRowValue = createRowValue();
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(oldRowValue), new SearchDataCriteria(), 1));

        RefBookRowValue newRowValue = createRowValue();
        newRowValue.setSystemId(oldRowValue.getSystemId() + 1);
        when(versionService.search(eq(TEST_REFBOOK_DRAFT_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(newRowValue), new SearchDataCriteria(), 1));

        UiDraft actual = controller.deleteDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, oldRowValue.getSystemId());
        assertEquals(uiDraft, actual);

        ArgumentCaptor<DeleteDataRequest> captor = ArgumentCaptor.forClass(DeleteDataRequest.class);
        verify(draftService, times(1))
                .deleteData(eq(TEST_REFBOOK_DRAFT_ID), captor.capture());

        DeleteDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_DRAFT_OPT_LOCK_VALUE), request.getOptLockValue());

        List<Row> rows = request.getRows();
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Row row = rows.get(0);
        assertNotNull(row);
        assertEquals(newRowValue.getSystemId(), row.getSystemId());
    }

    @Test
    public void testDeleteAllDataRecords() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.deleteAllDataRecords(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE);
        assertEquals(uiDraft, actual);

        ArgumentCaptor<DeleteAllDataRequest> captor = ArgumentCaptor.forClass(DeleteAllDataRequest.class);
        verify(draftService, times(1))
                .deleteAllData(eq(TEST_REFBOOK_VERSION_ID), captor.capture());

        DeleteAllDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_OPT_LOCK_VALUE), request.getOptLockValue());
    }

    @Test
    public void testDeleteAllDataRecordsWhenVersion() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        UiDraft actual = controller.deleteAllDataRecords(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE);
        assertEquals(uiDraft, actual);

        ArgumentCaptor<DeleteAllDataRequest> captor = ArgumentCaptor.forClass(DeleteAllDataRequest.class);
        verify(draftService, times(1))
                .deleteAllData(eq(TEST_REFBOOK_DRAFT_ID), captor.capture());

        DeleteAllDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_DRAFT_OPT_LOCK_VALUE), request.getOptLockValue());
    }

    @Test
    public void testCreateFromFile() {

        FileModel fileModel = new FileModel("path", "name");
        when(refBookService.create(eq(fileModel))).thenReturn(createDraft());

        RefBookVersion draftVersion = createDraftVersion();
        when(versionService.getById(eq(TEST_REFBOOK_DRAFT_ID))).thenReturn(draftVersion);

        UiDraft actual = controller.createFromFile(fileModel);
        assertEquals(new UiDraft(draftVersion), actual);
    }

    @Test
    public void testUploadFromFile() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        FileModel fileModel = new FileModel("path", "name");
        Draft draft = new Draft(version.getId(), "storageCode", version.getOptLockValue());
        when(draftService.create(eq(TEST_REFBOOK_ID), eq(fileModel))).thenReturn(draft);

        UiDraft actual = controller.uploadFromFile(TEST_REFBOOK_VERSION_ID, fileModel);
        assertEquals(new UiDraft(version), actual);
    }

    @Test
    public void testUploadData() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        FileModel fileModel = new FileModel("path", "name");
        UiDraft actual = controller.uploadData(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, fileModel);
        assertEquals(new UiDraft(version), actual);

        ArgumentCaptor<UpdateFromFileRequest> captor = ArgumentCaptor.forClass(UpdateFromFileRequest.class);
        verify(draftService, times(1))
                .updateFromFile(eq(TEST_REFBOOK_VERSION_ID), captor.capture());

        UpdateFromFileRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_OPT_LOCK_VALUE), request.getOptLockValue());
        assertEquals(fileModel, request.getFileModel());
    }

    /** Создание структуры с глубоким копированием атрибутов и ссылок. */
    private Structure createStructure() {

        Structure structure = new Structure(ATTRIBUTE_LIST, REFERENCE_LIST);
        return new Structure(structure);
    }

    private RefBookVersion createVersion() {

        RefBookVersion version = new RefBookVersion();
        version.setId(TEST_REFBOOK_VERSION_ID);
        version.setOptLockValue(TEST_OPT_LOCK_VALUE);

        version.setRefBookId(TEST_REFBOOK_ID);
        version.setCode(TEST_REFBOOK_CODE);

        version.setStructure(createStructure());

        return version;
    }

    private RefBookVersion createDraftVersion() {

        RefBookVersion version = new RefBookVersion();
        version.setId(TEST_REFBOOK_DRAFT_ID);
        version.setOptLockValue(TEST_DRAFT_OPT_LOCK_VALUE);

        version.setRefBookId(TEST_REFBOOK_ID);
        version.setCode(TEST_REFBOOK_CODE);

        version.setStatus(RefBookVersionStatus.DRAFT);

        version.setStructure(createStructure());

        return version;
    }

    private Draft createDraft() {

        return new Draft(TEST_REFBOOK_DRAFT_ID, "storageCode", TEST_DRAFT_OPT_LOCK_VALUE);
    }

    private UiDraft createUiDraft() {
        return new UiDraft(TEST_REFBOOK_DRAFT_ID, TEST_REFBOOK_ID, TEST_DRAFT_OPT_LOCK_VALUE);
    }

    private UiPassport createPassport() {

        UiPassport passport = new UiPassport();

        passport.setCode(TEST_REFBOOK_CODE);
        passport.setName(TEST_REFBOOK_NAME);
        passport.setShortName(TEST_REFBOOK_NAME.toLowerCase());
        passport.setDescription("RefBook " + TEST_REFBOOK_NAME);

        return passport;
    }

    private FormAttribute createFormAttribute() {

        FormAttribute formAttribute = new FormAttribute();

        formAttribute.setCode(NAME_ATTRIBUTE.getCode());
        formAttribute.setName(NAME_ATTRIBUTE.getName());
        formAttribute.setType(NAME_ATTRIBUTE.getType());

        return formAttribute;
    }

    private Row createRow(Long systemId) {

        Map<String, Object> map = new HashMap<>(3);
        map.put(ID_ATTRIBUTE_CODE, BigInteger.valueOf(1));
        map.put(NAME_ATTRIBUTE_CODE, "name");
        map.put(STRING_ATTRIBUTE_CODE, "string");

        return new Row(systemId, map);
    }

    private Row createEmptyRow(long systemId) {

        Map<String, Object> map = new HashMap<>(3);
        map.put(ID_ATTRIBUTE_CODE, null);
        map.put(NAME_ATTRIBUTE_CODE, "");
        map.put(STRING_ATTRIBUTE_CODE, null);

        return new Row(systemId, map);
    }

    private RefBookRowValue createRowValue() {

        LongRowValue longRowValue = new LongRowValue(TEST_SYSTEM_ID, asList(
                new IntegerFieldValue(ID_ATTRIBUTE_CODE, BigInteger.valueOf(TEST_SYSTEM_ID)),
                new StringFieldValue(NAME_ATTRIBUTE_CODE, "name_" + TEST_SYSTEM_ID),
                new StringFieldValue(STRING_ATTRIBUTE_CODE, "text with id = " + TEST_SYSTEM_ID)
        ));
        longRowValue.setHash(TEST_ROW_VALUE_HASH);

        return new RefBookRowValue(longRowValue, TEST_REFBOOK_VERSION_ID);
    }

    private Map<RefBookType, Map<Class<? extends UiStrategy>, UiStrategy>> getStrategies() {

        Map<RefBookType, Map<Class<? extends UiStrategy>, UiStrategy>> result = new HashMap<>();
        result.put(RefBookType.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getDefaultStrategies() {

        Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        result.put(FindOrCreateDraftStrategy.class, defaultFindOrCreateDraftStrategy);
        result.put(ValidateIsDraftStrategy.class, defaultValidateIsDraftStrategy);

        return result;
    }
}