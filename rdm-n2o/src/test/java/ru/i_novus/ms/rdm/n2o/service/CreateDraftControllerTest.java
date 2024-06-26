package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.n2o.BaseTest;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;
import ru.i_novus.ms.rdm.n2o.model.FormAttribute;
import ru.i_novus.ms.rdm.n2o.model.UiPassport;
import ru.i_novus.ms.rdm.n2o.strategy.BaseUiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.draft.DefaultFindOrCreateDraftStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.draft.FindOrCreateDraftStrategy;
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
import static org.springframework.test.util.ReflectionTestUtils.setField;
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

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {

        final UiStrategyLocator strategyLocator = new BaseUiStrategyLocator(getStrategies());
        setField(controller, "strategyLocator", strategyLocator);
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

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.editPassport(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, passport);
        assertEquals(uiDraft, actual);

        final ArgumentCaptor<RefBookUpdateRequest> captor = ArgumentCaptor.forClass(RefBookUpdateRequest.class);
        verify(refBookService, times(1)).update(captor.capture());

        final RefBookUpdateRequest request = captor.getValue();
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

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.editPassport(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createPassport());
        assertEquals(uiDraft, actual);
    }

    @Test
    public void testEditPassportWhenCreateDraft() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.editPassport(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createPassport());
        assertEquals(uiDraft, actual);
    }

    @Test
    public void testCreateAttribute() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.createAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .createAttribute(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testCreateAttributeWhenVersion() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.createAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .createAttribute(eq(TEST_REFBOOK_DRAFT_ID), eq(TEST_DRAFT_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testUpdateAttribute() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.updateAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .updateAttribute(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testUpdateAttributeWhenVersion() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.updateAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, createFormAttribute());
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .updateAttribute(eq(TEST_REFBOOK_DRAFT_ID), eq(TEST_DRAFT_OPT_LOCK_VALUE), any(FormAttribute.class));
    }

    @Test
    public void testDeleteAttribute() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.deleteAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, NAME_ATTRIBUTE_CODE);
        assertEquals(uiDraft, actual);

        verify(structureController, times(1))
                .deleteAttribute(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), eq(NAME_ATTRIBUTE_CODE));
    }

    @Test
    public void testDeleteAttributeWhenVersion() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.deleteAttribute(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, NAME_ATTRIBUTE_CODE);
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

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final Row row = createRow(null);
        when(versionService.getStructure(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version.getStructure());

        if (rowValue != null) {
            final SearchDataCriteria criteria = new SearchDataCriteria();
            criteria.setPageSize(1);

            when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                    .thenReturn(new PageImpl<>(emptyList(), criteria, 1));
        }

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
        assertEquals(uiDraft, actual);

        verify(dataRecordController, times(1))
                .updateData(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(Row.class));
    }

    @Test
    @SuppressWarnings("java:S5778")
    public void testCreateDataRecordFailed() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft( version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final Row row = createRow(null);
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

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final Row row = createRow(1L);
        UiDraft actual = controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
        assertEquals(uiDraft, actual);

        verify(dataRecordController, times(1))
                .updateData(eq(TEST_REFBOOK_VERSION_ID), eq(TEST_OPT_LOCK_VALUE), any(Row.class));
    }

    @Test
    public void testUpdateDataRecordWhenVersion() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final RefBookRowValue oldRowValue = createRowValue();
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(oldRowValue), new SearchDataCriteria(), 1));

        final RefBookRowValue newRowValue = createRowValue();
        newRowValue.setSystemId(oldRowValue.getSystemId() + 1);
        when(versionService.search(eq(TEST_REFBOOK_DRAFT_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(newRowValue), new SearchDataCriteria(), 1));

        final Row row = createRow(1L);
        final UiDraft actual = controller.updateDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, row);
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

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.deleteDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, TEST_SYSTEM_ID);
        assertEquals(uiDraft, actual);

        final ArgumentCaptor<DeleteDataRequest> captor = ArgumentCaptor.forClass(DeleteDataRequest.class);
        verify(draftService, times(1))
                .deleteData(eq(TEST_REFBOOK_VERSION_ID), captor.capture());

        final DeleteDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_OPT_LOCK_VALUE), request.getOptLockValue());

        final List<Row> rows = request.getRows();
        assertNotNull(rows);
        assertEquals(1, rows.size());

        final Row row = rows.get(0);
        assertNotNull(row);
        assertEquals(Long.valueOf(TEST_SYSTEM_ID), row.getSystemId());
    }

    @Test
    public void testDeleteDataRecordWhenVersion() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final RefBookRowValue oldRowValue = createRowValue();
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(oldRowValue), new SearchDataCriteria(), 1));

        final RefBookRowValue newRowValue = createRowValue();
        newRowValue.setSystemId(oldRowValue.getSystemId() + 1);
        when(versionService.search(eq(TEST_REFBOOK_DRAFT_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(newRowValue), new SearchDataCriteria(), 1));

        final UiDraft actual = controller.deleteDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, oldRowValue.getSystemId());
        assertEquals(uiDraft, actual);

        final ArgumentCaptor<DeleteDataRequest> captor = ArgumentCaptor.forClass(DeleteDataRequest.class);
        verify(draftService, times(1))
                .deleteData(eq(TEST_REFBOOK_DRAFT_ID), captor.capture());

        final DeleteDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_DRAFT_OPT_LOCK_VALUE), request.getOptLockValue());

        final List<Row> rows = request.getRows();
        assertNotNull(rows);
        assertEquals(1, rows.size());

        final Row row = rows.get(0);
        assertNotNull(row);
        assertEquals(newRowValue.getSystemId(), row.getSystemId());
    }

    @Test
    public void testDeleteAllDataRecords() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.deleteAllDataRecords(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE);
        assertEquals(uiDraft, actual);

        final ArgumentCaptor<DeleteAllDataRequest> captor = ArgumentCaptor.forClass(DeleteAllDataRequest.class);
        verify(draftService, times(1))
                .deleteAllData(eq(TEST_REFBOOK_VERSION_ID), captor.capture());

        final DeleteAllDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_OPT_LOCK_VALUE), request.getOptLockValue());
    }

    @Test
    public void testDeleteAllDataRecordsWhenVersion() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = createUiDraft();
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final UiDraft actual = controller.deleteAllDataRecords(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE);
        assertEquals(uiDraft, actual);

        final ArgumentCaptor<DeleteAllDataRequest> captor = ArgumentCaptor.forClass(DeleteAllDataRequest.class);
        verify(draftService, times(1))
                .deleteAllData(eq(TEST_REFBOOK_DRAFT_ID), captor.capture());

        final DeleteAllDataRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_DRAFT_OPT_LOCK_VALUE), request.getOptLockValue());
    }

    @Test
    public void testCreateFromFile() {

        final FileModel fileModel = new FileModel("path", "name");
        when(refBookService.create(eq(fileModel))).thenReturn(createDraft());

        final RefBookVersion draftVersion = createDraftVersion();
        when(versionService.getById(eq(TEST_REFBOOK_DRAFT_ID))).thenReturn(draftVersion);

        final UiDraft actual = controller.createFromFile(fileModel);
        assertEquals(new UiDraft(draftVersion), actual);
    }

    @Test
    public void testUploadFromFile() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final FileModel fileModel = new FileModel("path", "name");
        final Draft draft = new Draft(version.getId(), "storageCode", version.getOptLockValue());
        when(draftService.create(eq(TEST_REFBOOK_ID), eq(fileModel))).thenReturn(draft);

        final UiDraft actual = controller.uploadFromFile(TEST_REFBOOK_VERSION_ID, fileModel);
        assertEquals(new UiDraft(version), actual);
    }

    @Test
    public void testUploadData() {

        final RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        final UiDraft uiDraft = new UiDraft(version);
        when(defaultFindOrCreateDraftStrategy.findOrCreate(eq(version))).thenReturn(uiDraft);

        final FileModel fileModel = new FileModel("path", "name");
        final UiDraft actual = controller.uploadData(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, fileModel);
        assertEquals(uiDraft, actual);

        final ArgumentCaptor<UpdateFromFileRequest> captor = ArgumentCaptor.forClass(UpdateFromFileRequest.class);
        verify(draftService, times(1))
                .updateFromFile(eq(TEST_REFBOOK_VERSION_ID), captor.capture());

        final UpdateFromFileRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals(Integer.valueOf(TEST_OPT_LOCK_VALUE), request.getOptLockValue());
        assertEquals(fileModel, request.getFileModel());
    }

    private RefBookVersion createVersion() {

        final RefBookVersion version = new RefBookVersion();
        version.setId(TEST_REFBOOK_VERSION_ID);
        version.setOptLockValue(TEST_OPT_LOCK_VALUE);

        version.setRefBookId(TEST_REFBOOK_ID);
        version.setCode(TEST_REFBOOK_CODE);

        version.setStructure(createStructure());

        return version;
    }

    private RefBookVersion createDraftVersion() {

        final RefBookVersion version = new RefBookVersion();
        version.setId(TEST_REFBOOK_DRAFT_ID);
        version.setOptLockValue(TEST_DRAFT_OPT_LOCK_VALUE);

        version.setRefBookId(TEST_REFBOOK_ID);
        version.setCode(TEST_REFBOOK_CODE);

        version.setStatus(RefBookVersionStatus.DRAFT);

        version.setStructure(createStructure());

        return version;
    }

    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }

    private Draft createDraft() {

        return new Draft(TEST_REFBOOK_DRAFT_ID, "storageCode", TEST_DRAFT_OPT_LOCK_VALUE);
    }

    private UiDraft createUiDraft() {

        return new UiDraft(TEST_REFBOOK_DRAFT_ID, TEST_REFBOOK_ID, TEST_DRAFT_OPT_LOCK_VALUE);
    }

    private UiPassport createPassport() {

        final UiPassport passport = new UiPassport();
        passport.setCode(TEST_REFBOOK_CODE);
        passport.setName(TEST_REFBOOK_NAME);

        passport.setShortName(TEST_REFBOOK_NAME.toLowerCase());
        passport.setDescription("RefBook " + TEST_REFBOOK_NAME);

        return passport;
    }

    private FormAttribute createFormAttribute() {

        final FormAttribute formAttribute = new FormAttribute();
        formAttribute.setCode(NAME_ATTRIBUTE.getCode());
        formAttribute.setName(NAME_ATTRIBUTE.getName());
        formAttribute.setType(NAME_ATTRIBUTE.getType());

        return formAttribute;
    }

    private Row createRow(Long systemId) {

        final Map<String, Object> map = new HashMap<>(3);
        map.put(ID_ATTRIBUTE_CODE, BigInteger.valueOf(1));
        map.put(NAME_ATTRIBUTE_CODE, "name");
        map.put(STRING_ATTRIBUTE_CODE, "string");

        return new Row(systemId, map);
    }

    private Row createEmptyRow(long systemId) {

        final Map<String, Object> map = new HashMap<>(3);
        map.put(ID_ATTRIBUTE_CODE, null);
        map.put(NAME_ATTRIBUTE_CODE, "");
        map.put(STRING_ATTRIBUTE_CODE, null);

        return new Row(systemId, map);
    }

    private RefBookRowValue createRowValue() {

        final LongRowValue longRowValue = new LongRowValue(TEST_SYSTEM_ID, asList(
                new IntegerFieldValue(ID_ATTRIBUTE_CODE, BigInteger.valueOf(TEST_SYSTEM_ID)),
                new StringFieldValue(NAME_ATTRIBUTE_CODE, "name_" + TEST_SYSTEM_ID),
                new StringFieldValue(STRING_ATTRIBUTE_CODE, "text with id = " + TEST_SYSTEM_ID)
        ));
        longRowValue.setHash(TEST_ROW_VALUE_HASH);

        return new RefBookRowValue(longRowValue, TEST_REFBOOK_VERSION_ID);
    }

    private Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> getStrategies() {

        final Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getDefaultStrategies() {

        final Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        result.put(FindOrCreateDraftStrategy.class, defaultFindOrCreateDraftStrategy);

        return result;
    }
}