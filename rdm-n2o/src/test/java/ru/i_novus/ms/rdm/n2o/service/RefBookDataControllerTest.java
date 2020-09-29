package ru.i_novus.ms.rdm.n2o.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataDecorator;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.n2o.service.RefBookDataController.EMPTY_SEARCH_DATA_CRITERIA;
import static ru.i_novus.ms.rdm.n2o.utils.UiRefBookTestUtils.assertObjects;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("SameParameterValue")
public class RefBookDataControllerTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final int TEST_OPT_LOCK_VALUE = 10;
    private static final int NEW_OPT_LOCK_VALUE = TEST_OPT_LOCK_VALUE + 1;

    private static final String ATTRIBUTE_ID_CODE = "id";
    private static final String ATTRIBUTE_NAME_CODE = "name";
    private static final String ATTRIBUTE_TEXT_CODE = "text";

    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RefBookDataController refBookDataController;

    @Mock
    private DataFieldFilterProvider dataFieldFilterProvider;

    @Mock
    private VersionRestService versionService;

    @Mock
    private ConflictService conflictService;

    @Mock
    private RefBookDataDecorator refBookDataDecorator;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testGetList() {

        RefBookVersion version = createRefBookVersion(TEST_REFBOOK_VERSION_ID);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        DataCriteria criteria = createCriteria(TEST_REFBOOK_VERSION_ID, false);
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize());

        List<RefBookRowValue> rowValues = createContent(TEST_REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(rowValuesPage);

        when(refBookDataDecorator.getDataStructure(eq(TEST_REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());
        when(refBookDataDecorator.getDataContent(eq(rowValues), eq(criteria))).thenReturn(rowValues);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        Page<RefBookDataController.DataGridRow> dataGridRows = refBookDataController.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // За вычетом "записи"-заголовка.
        assertEquals(rowValues.size(), dataGridRows.getContent().size() - 1);
    }

    @Test
    public void testGetConflictedListWithoutConflicts() {

        RefBookVersion version = createRefBookVersion(TEST_REFBOOK_VERSION_ID);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        when(conflictService.countConflictedRowIds(any(RefBookConflictCriteria.class))).thenReturn(0L);

        DataCriteria criteria = createCriteria(TEST_REFBOOK_VERSION_ID, true);
        Page<RefBookDataController.DataGridRow> dataGridRows = refBookDataController.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        assertEquals(0, dataGridRows.getContent().size());
    }

    @Test
    public void testGetConflictedListWithConflict() {

        RefBookVersion version = createRefBookVersion(TEST_REFBOOK_VERSION_ID);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        when(conflictService.countConflictedRowIds(any(RefBookConflictCriteria.class))).thenReturn(1L);

        List<RefBookRowValue> rowValues = createContent(TEST_REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, EMPTY_SEARCH_DATA_CRITERIA, rowValues.size());
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), eq(EMPTY_SEARCH_DATA_CRITERIA))).thenReturn(rowValuesPage);

        List<Long> conflictedRowIds = List.of(1L);
        RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        when(conflictService.searchConflictedRowIds(any(RefBookConflictCriteria.class)))
                .thenReturn(new PageImpl<>(conflictedRowIds, conflictCriteria, 1));

        DataCriteria criteria = createCriteria(TEST_REFBOOK_VERSION_ID, true);
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize());
        searchDataCriteria.addAttributeFilterList(emptyList());
        searchDataCriteria.setRowSystemIds(conflictedRowIds);

        List<RefBookRowValue> conflictedRowValues = List.of(rowValues.get(0));
        Page<RefBookRowValue> conflictedRowValuesPage = new PageImpl<>(conflictedRowValues, searchDataCriteria, conflictedRowValues.size());
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), eq(searchDataCriteria))).thenReturn(conflictedRowValuesPage);

        when(refBookDataDecorator.getDataStructure(eq(TEST_REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());
        when(refBookDataDecorator.getDataContent(eq(conflictedRowValues), eq(criteria)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        Page<RefBookDataController.DataGridRow> dataGridRows = refBookDataController.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // За вычетом "записи"-заголовка.
        assertEquals(conflictedRowValues.size(), dataGridRows.getContent().size() - 1);
    }

    @Test
    public void testGetVersion() {

        RefBookVersion version = createRefBookVersion(TEST_REFBOOK_VERSION_ID);
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        RefBookVersion expected = new RefBookVersion(version);

        RefBookVersion actual1 = refBookDataController.getVersion(TEST_REFBOOK_VERSION_ID, null);
        assertVersions(expected, actual1);
        assertEquals(expected.getOptLockValue(), actual1.getOptLockValue());
        assertVersions(expected, version);
        assertEquals(expected.getOptLockValue(), actual1.getOptLockValue());

        RefBookVersion actual2 = refBookDataController.getVersion(TEST_REFBOOK_VERSION_ID, NEW_OPT_LOCK_VALUE);
        assertVersions(expected, actual2);
        assertNotEquals(expected.getOptLockValue(), actual2.getOptLockValue());
        assertVersions(expected, version);
        assertNotEquals(expected.getOptLockValue(), version.getOptLockValue());
    }

    private DataCriteria createCriteria(int versionId, boolean hasDataConflict) {

        DataCriteria criteria = new DataCriteria();

        criteria.setVersionId(versionId);
        criteria.setOptLockValue(TEST_OPT_LOCK_VALUE);
        criteria.setHasDataConflict(hasDataConflict);

        return criteria;
    }

    private RefBookVersion createRefBookVersion(int versionId) {

        RefBookVersion version = new RefBookVersion();

        version.setId(versionId);
        version.setOptLockValue(TEST_OPT_LOCK_VALUE);
        version.setStructure(createStructure());

        return version;
    }

    private void assertVersions(RefBookVersion expected, RefBookVersion actual) {

        assertEquals(expected.getId(), actual.getId());
        assertObjects(Assert::assertEquals, expected.getStructure(), actual.getStructure());
    }

    private Structure createStructure() {

        return new Structure(asList(
                Structure.Attribute.buildPrimary(ATTRIBUTE_ID_CODE, "Идентификатор", FieldType.INTEGER, null),
                Structure.Attribute.build(ATTRIBUTE_NAME_CODE, "Наименование", FieldType.STRING, null),
                Structure.Attribute.build(ATTRIBUTE_TEXT_CODE, "Текст", FieldType.STRING, null)
        ), null);
    }

    private List<RefBookRowValue> createContent(int versionId) {

        int rowValueCount = 10;

        List<RefBookRowValue> rowValues = new ArrayList<>(rowValueCount);

        LongStream.range(1, rowValueCount + 1).forEach(systemId -> {
            LongRowValue longRowValue = new LongRowValue(systemId, asList(
                    new IntegerFieldValue(ATTRIBUTE_ID_CODE, BigInteger.valueOf(systemId)),
                    new StringFieldValue(ATTRIBUTE_NAME_CODE, "name_" + systemId),
                    new StringFieldValue(ATTRIBUTE_TEXT_CODE, "text with id = " + systemId)
            ));
            rowValues.add(new RefBookRowValue(longRowValue, versionId));
        });

        return rowValues;
    }
}