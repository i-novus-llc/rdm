package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataDecorator;
import ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils;
import ru.i_novus.ms.rdm.n2o.util.RefBookDataUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.*;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.EXACT;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.LIKE;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("SameParameterValue")
public class RefBookDataControllerTest  {

    private static final int REFBOOK_VERSION_ID = -10;
    private static final int OPT_LOCK_VALUE = 10;
    private static final int NEW_OPT_LOCK_VALUE = OPT_LOCK_VALUE + 1;

    private static final String TEST_LOCALE_CODE = "test";

    private static final SearchDataCriteria EMPTY_SEARCH_DATA_CRITERIA = new SearchDataCriteria(0, 1);
    private static final List<FieldType> LIKE_FIELD_TYPES = List.of(FieldType.STRING, FieldType.REFERENCE);

    @InjectMocks
    private RefBookDataController controller;

    @Mock
    private DataFieldFilterProvider dataFieldFilterProvider;

    @Mock
    private VersionRestService versionService;

    @Mock
    private ConflictService conflictService;

    @Mock
    private RefBookDataDecorator refBookDataDecorator;

    @Test
    public void testGetList() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        DataCriteria criteria = createCriteria(false);
        criteria.setFilter(createCriteriaFilter());
        criteria.setOrders(createSortOrders());

        SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);

        List<AttributeFilter> filters = criteria.getFilter().entrySet().stream()
                .map(e -> toAttributeFilter(version.getStructure(), e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .collect(toList());
        searchDataCriteria.addAttributeFilterList(filters);

        Sort.Order order = new Sort.Order(Sort.Direction.ASC, ID_ATTRIBUTE_CODE);
        searchDataCriteria.setOrders(singletonList(order));

        List<RefBookRowValue> rowValues = createContent(REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), eq(searchDataCriteria))).thenReturn(rowValuesPage);

        when(refBookDataDecorator.getDataStructure(eq(REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());
        when(refBookDataDecorator.getDataContent(eq(rowValues), eq(criteria))).thenReturn(rowValues);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        Page<RefBookDataController.DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // За вычетом "записи"-заголовка.
        assertEquals(rowValues.size(), dataGridRows.getContent().size() - 1);

        verify(versionService)
                .search(eq(REFBOOK_VERSION_ID), any(SearchDataCriteria.class));
    }

    @Test
    public void testGetListWithLocaleCode() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        DataCriteria criteria = createCriteria(false);
        criteria.setLocaleCode(TEST_LOCALE_CODE);
        criteria.setFilter(createCriteriaFilter());

        SearchDataCriteria localeDataCriteria = createSearchDataCriteria(criteria);
        localeDataCriteria.setLocaleCode(TEST_LOCALE_CODE);

        List<AttributeFilter> filters = criteria.getFilter().entrySet().stream()
                .map(e -> toAttributeFilter(version.getStructure(), e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .collect(toList());
        localeDataCriteria.addAttributeFilterList(filters);

        SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);
        searchDataCriteria.setLocaleCode(TEST_LOCALE_CODE);
        searchDataCriteria.addAttributeFilterList(filters);

        List<RefBookRowValue> rowValues = createContent(REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(emptyList())) // origin storage
                .thenReturn(rowValuesPage); // locale storage

        when(refBookDataDecorator.getDataStructure(eq(REFBOOK_VERSION_ID), eq(criteria)))
                .thenReturn(version.getStructure());

        when(refBookDataDecorator.getDataContent(eq(rowValues), eq(criteria)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        Page<RefBookDataController.DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // За вычетом "записи"-заголовка.
        assertEquals(rowValues.size(), dataGridRows.getContent().size() - 1);

        verify(versionService, times(2))
                .search(eq(REFBOOK_VERSION_ID), any(SearchDataCriteria.class));
    }

    @Test
    public void testGetConflictedListWithoutConflicts() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        when(conflictService.countConflictedRowIds(any(RefBookConflictCriteria.class))).thenReturn(0L);

        DataCriteria criteria = createCriteria(true);
        Page<RefBookDataController.DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        assertEquals(0, dataGridRows.getContent().size());
    }

    @Test
    public void testGetConflictedListWithConflict() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        when(conflictService.countConflictedRowIds(any(RefBookConflictCriteria.class))).thenReturn(1L);

        List<RefBookRowValue> rowValues = createContent(REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, EMPTY_SEARCH_DATA_CRITERIA, rowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), eq(EMPTY_SEARCH_DATA_CRITERIA))).thenReturn(rowValuesPage);

        List<Long> conflictedRowIds = List.of(1L);
        RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        when(conflictService.searchConflictedRowIds(any(RefBookConflictCriteria.class)))
                .thenReturn(new PageImpl<>(conflictedRowIds, conflictCriteria, 1));

        DataCriteria criteria = createCriteria(true);
        SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);
        searchDataCriteria.setAttributeFilters(emptySet());
        searchDataCriteria.setRowSystemIds(conflictedRowIds);

        List<RefBookRowValue> conflictedRowValues = List.of(rowValues.get(0));
        Page<RefBookRowValue> conflictedRowValuesPage = new PageImpl<>(conflictedRowValues, searchDataCriteria, conflictedRowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), eq(searchDataCriteria))).thenReturn(conflictedRowValuesPage);

        when(refBookDataDecorator.getDataStructure(eq(REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());
        when(refBookDataDecorator.getDataContent(eq(conflictedRowValues), eq(criteria)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        Page<RefBookDataController.DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // За вычетом "записи"-заголовка.
        assertEquals(conflictedRowValues.size(), dataGridRows.getContent().size() - 1);
    }

    @Test
    public void testGetVersion() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        RefBookVersion expected = new RefBookVersion(version);

        RefBookVersion actual1 = controller.getVersion(REFBOOK_VERSION_ID, null);
        assertVersions(expected, actual1);
        assertEquals(expected.getOptLockValue(), actual1.getOptLockValue());
        assertVersions(expected, version);
        assertEquals(expected.getOptLockValue(), actual1.getOptLockValue());

        RefBookVersion actual2 = controller.getVersion(REFBOOK_VERSION_ID, NEW_OPT_LOCK_VALUE);
        assertVersions(expected, actual2);
        assertNotEquals(expected.getOptLockValue(), actual2.getOptLockValue());
        assertVersions(expected, version);
        assertNotEquals(expected.getOptLockValue(), version.getOptLockValue());
    }

    private DataCriteria createCriteria(boolean hasDataConflict) {

        DataCriteria criteria = new DataCriteria();

        criteria.setVersionId(REFBOOK_VERSION_ID);
        criteria.setOptLockValue(OPT_LOCK_VALUE);
        criteria.setHasDataConflict(hasDataConflict);

        return criteria;
    }

    private Map<String, Serializable> createCriteriaFilter() {

        Map<String, Serializable> filter = new HashMap<>();
        filter.put("", "empty");
        filter.put(ID_ATTRIBUTE_CODE, "1");
        filter.put(NAME_ATTRIBUTE_CODE, null);
        filter.put(STRING_ATTRIBUTE_CODE, "text_1");
        filter.put(INTEGER_ATTRIBUTE_CODE, "11");
        filter.put(FLOAT_ATTRIBUTE_CODE, "1.1");
        filter.put(BOOLEAN_ATTRIBUTE_CODE, "false");
        filter.put(DATE_ATTRIBUTE_CODE, "11.11.1111 11:11:11");
        filter.put(REFERENCE_ATTRIBUTE_CODE, "REFER_1");
        filter.put(SELF_REFER_ATTRIBUTE_CODE, "SELF_1");

        return filter;
    }

    private List<Sort.Order> createSortOrders() {

        Sort.Order order = new Sort.Order(Sort.Direction.ASC, DataRecordUtils.addPrefix(ID_ATTRIBUTE_CODE));
        return singletonList(order);
    }

    private SearchDataCriteria createSearchDataCriteria(DataCriteria criteria) {

        return new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize());
    }

    private AttributeFilter toAttributeFilter(Structure structure, String filterName, Serializable filterValue) {

        if (filterValue == null || StringUtils.isEmpty(filterName))
            return null;

        Structure.Attribute attribute = structure.getAttribute(filterName);
        Serializable attributeValue = RefBookDataUtils.castFilterValue(attribute, filterValue);
        if (attributeValue == null)
            return null;

        AttributeFilter attributeFilter = new AttributeFilter(filterName, attributeValue, attribute.getType());
        attributeFilter.setSearchType(toSearchType(attribute));
        return attributeFilter;
    }

    private SearchTypeEnum toSearchType(Structure.Attribute attribute) {
        return LIKE_FIELD_TYPES.contains(attribute.getType()) ? LIKE : EXACT;
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion version = new RefBookVersion();

        version.setId(REFBOOK_VERSION_ID);
        version.setOptLockValue(OPT_LOCK_VALUE);
        version.setStructure(createStructure());

        return version;
    }

    private void assertVersions(RefBookVersion expected, RefBookVersion actual) {

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getStructure(), actual.getStructure());
    }

    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }

    private List<RefBookRowValue> createContent(int versionId) {

        int rowValueCount = 10;

        List<RefBookRowValue> rowValues = new ArrayList<>(rowValueCount);

        LongStream.range(1, rowValueCount + 1).forEach(systemId ->
                rowValues.add(new RefBookRowValue(createLongRowValue(systemId), versionId))
        );

        return rowValues;
    }

    private LongRowValue createLongRowValue(long systemId) {

        return new LongRowValue(systemId, asList(
                new IntegerFieldValue(ID_ATTRIBUTE_CODE, BigInteger.valueOf(systemId)),
                new StringFieldValue(NAME_ATTRIBUTE_CODE, "name_" + systemId),
                new StringFieldValue(STRING_ATTRIBUTE_CODE, "text with id = " + systemId),
                new IntegerFieldValue(INTEGER_ATTRIBUTE_CODE, Math.toIntExact(systemId)),
                new FloatFieldValue(FLOAT_ATTRIBUTE_CODE, BigDecimal.valueOf(systemId * 1.0 / 10)),
                new BooleanFieldValue(BOOLEAN_ATTRIBUTE_CODE, systemId > 10),
                new DateFieldValue(DATE_ATTRIBUTE_CODE, LocalDate.now(ZoneId.of("UTC"))),
                new ReferenceFieldValue(REFERENCE_ATTRIBUTE_CODE,
                        new Reference(String.valueOf(systemId), "display_" + systemId)),
                new ReferenceFieldValue(SELF_REFER_ATTRIBUTE_CODE,
                        new Reference(null, null))
        ));
    }
}