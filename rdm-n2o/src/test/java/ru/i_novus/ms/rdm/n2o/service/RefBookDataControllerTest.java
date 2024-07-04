package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import net.n2oapp.platform.i18n.Messages;
import org.junit.Before;
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
import ru.i_novus.ms.rdm.n2o.model.grid.DataGridRow;
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
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.*;
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

    @Mock
    private Messages messages;

    @Before
    public void setUp() {

        when(messages.getMessage(any(String.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void testGetList() {

        final RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        final DataCriteria criteria = createCriteria(false);
        criteria.setFilter(createCriteriaFilter());
        criteria.setOrders(createSortOrders());

        final SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);

        final List<AttributeFilter> filters = criteria.getFilter().entrySet().stream()
                .map(e -> toAttributeFilter(version.getStructure(), e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .collect(toList());
        searchDataCriteria.addAttributeFilterList(filters);

        final Sort.Order order = new Sort.Order(Sort.Direction.ASC, ID_ATTRIBUTE_CODE);
        searchDataCriteria.setOrders(singletonList(order));

        final List<RefBookRowValue> rowValues = createContent(REFBOOK_VERSION_ID);
        final Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), eq(searchDataCriteria))).thenReturn(rowValuesPage);

        when(refBookDataDecorator.getDataStructure(eq(REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());
        when(refBookDataDecorator.getDataContent(eq(rowValues), eq(criteria))).thenReturn(rowValues);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        final Page<DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        assertEquals(rowValues.size(), dataGridRows.getContent().size());

        verify(versionService)
                .search(eq(REFBOOK_VERSION_ID), any(SearchDataCriteria.class));
    }

    @Test
    public void testGetListWithLocaleCode() {

        final RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        final DataCriteria criteria = createCriteria(false);
        criteria.setLocaleCode(TEST_LOCALE_CODE);
        criteria.setFilter(createCriteriaFilter());

        final SearchDataCriteria localeDataCriteria = createSearchDataCriteria(criteria);
        localeDataCriteria.setLocaleCode(TEST_LOCALE_CODE);

        final List<AttributeFilter> filters = criteria.getFilter().entrySet().stream()
                .map(e -> toAttributeFilter(version.getStructure(), e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .collect(toList());
        localeDataCriteria.addAttributeFilterList(filters);

        final SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);
        searchDataCriteria.setLocaleCode(TEST_LOCALE_CODE);
        searchDataCriteria.addAttributeFilterList(filters);

        final List<RefBookRowValue> rowValues = createContent(REFBOOK_VERSION_ID);
        final Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), any(SearchDataCriteria.class)))
                .thenReturn(new PageImpl<>(emptyList())) // origin storage
                .thenReturn(rowValuesPage); // locale storage

        when(refBookDataDecorator.getDataStructure(eq(REFBOOK_VERSION_ID), eq(criteria)))
                .thenReturn(version.getStructure());

        when(refBookDataDecorator.getDataContent(eq(rowValues), eq(criteria)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        final Page<DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        assertEquals(rowValues.size(), dataGridRows.getContent().size());

        verify(versionService, times(2))
                .search(eq(REFBOOK_VERSION_ID), any(SearchDataCriteria.class));
    }

    @Test
    public void testGetConflictedListWithoutConflicts() {

        final RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        when(conflictService.countConflictedRowIds(any(RefBookConflictCriteria.class))).thenReturn(0L);

        final DataCriteria criteria = createCriteria(true);
        final Page<DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        assertEquals(0, dataGridRows.getContent().size());
    }

    @Test
    public void testGetConflictedListWithConflict() {

        final RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        when(conflictService.countConflictedRowIds(any(RefBookConflictCriteria.class))).thenReturn(1L);

        final List<RefBookRowValue> rowValues = createContent(REFBOOK_VERSION_ID);
        final Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, EMPTY_SEARCH_DATA_CRITERIA, rowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), eq(EMPTY_SEARCH_DATA_CRITERIA))).thenReturn(rowValuesPage);

        final List<Long> conflictedRowIds = List.of(1L);
        final RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        when(conflictService.searchConflictedRowIds(any(RefBookConflictCriteria.class)))
                .thenReturn(new PageImpl<>(conflictedRowIds, conflictCriteria, 1));

        final DataCriteria criteria = createCriteria(true);
        final SearchDataCriteria searchDataCriteria = createSearchDataCriteria(criteria);
        searchDataCriteria.setAttributeFilters(emptySet());
        searchDataCriteria.setRowSystemIds(conflictedRowIds);

        final List<RefBookRowValue> conflictedRowValues = List.of(rowValues.get(0));
        final Page<RefBookRowValue> conflictedRowValuesPage = new PageImpl<>(conflictedRowValues, searchDataCriteria, conflictedRowValues.size());
        when(versionService.search(eq(REFBOOK_VERSION_ID), eq(searchDataCriteria))).thenReturn(conflictedRowValuesPage);

        when(refBookDataDecorator.getDataStructure(eq(REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());
        when(refBookDataDecorator.getDataContent(eq(conflictedRowValues), eq(criteria)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        final Page<DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        assertEquals(conflictedRowValues.size(), dataGridRows.getContent().size());
    }

    @Test
    public void testGetVersion() {

        final RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(REFBOOK_VERSION_ID))).thenReturn(version);

        final RefBookVersion expected = new RefBookVersion(version);

        final RefBookVersion actual = controller.getVersion(REFBOOK_VERSION_ID, null);
        assertVersions(expected, actual);
        assertEquals(expected.getOptLockValue(), actual.getOptLockValue());
        assertVersions(expected, version);
        assertEquals(expected.getOptLockValue(), actual.getOptLockValue());

        final RefBookVersion actualWithOptLock = controller.getVersion(REFBOOK_VERSION_ID, NEW_OPT_LOCK_VALUE);
        assertVersions(expected, actualWithOptLock);
        assertNotEquals(expected.getOptLockValue(), actualWithOptLock.getOptLockValue());
        assertVersions(expected, version);
        assertNotEquals(expected.getOptLockValue(), version.getOptLockValue());
    }

    private DataCriteria createCriteria(boolean hasDataConflict) {

        final DataCriteria criteria = new DataCriteria();
        criteria.setVersionId(REFBOOK_VERSION_ID);
        criteria.setOptLockValue(OPT_LOCK_VALUE);
        criteria.setHasDataConflict(hasDataConflict);

        return criteria;
    }

    private Map<String, Serializable> createCriteriaFilter() {

        final Map<String, Serializable> filter = new HashMap<>(10);
        filter.put("", "empty");
        filter.put(addPrefix(ID_ATTRIBUTE_CODE), "1");
        filter.put(addPrefix(NAME_ATTRIBUTE_CODE), null);
        filter.put(addPrefix(STRING_ATTRIBUTE_CODE), "text_1");
        filter.put(addPrefix(INTEGER_ATTRIBUTE_CODE), "11");
        filter.put(addPrefix(FLOAT_ATTRIBUTE_CODE), "1.1");
        filter.put(addPrefix(BOOLEAN_ATTRIBUTE_CODE), "false");
        filter.put(addPrefix(DATE_ATTRIBUTE_CODE), "11.11.1111 11:11:11");
        filter.put(addPrefix(REFERENCE_ATTRIBUTE_CODE), "REFER_1");
        filter.put(addPrefix(SELF_REFER_ATTRIBUTE_CODE), "SELF_1");

        return filter;
    }

    private List<Sort.Order> createSortOrders() {

        final Sort.Order order = new Sort.Order(Sort.Direction.ASC, DataRecordUtils.addPrefix(ID_ATTRIBUTE_CODE));

        return singletonList(order);
    }

    private SearchDataCriteria createSearchDataCriteria(DataCriteria criteria) {

        return new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize());
    }

    private AttributeFilter toAttributeFilter(Structure structure, String filterName, Serializable filterValue) {

        if (filterValue == null || StringUtils.isEmpty(filterName) || !hasPrefix(filterName))
            return null;

        final String attributeCode = deletePrefix(filterName);
        final Structure.Attribute attribute = structure.getAttribute(attributeCode);
        if (attribute == null)
            return null;

        final Serializable attributeValue = RefBookDataUtils.castFilterValue(attribute, filterValue);
        if (attributeValue == null)
            return null;

        final AttributeFilter attributeFilter = new AttributeFilter(attributeCode, attributeValue, attribute.getType());
        attributeFilter.setSearchType(toSearchType(attribute));
        return attributeFilter;
    }

    private SearchTypeEnum toSearchType(Structure.Attribute attribute) {

        return LIKE_FIELD_TYPES.contains(attribute.getType()) ? LIKE : EXACT;
    }

    private RefBookVersion createRefBookVersion() {

        final RefBookVersion version = new RefBookVersion();
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

        final int rowValueCount = 10;
        final List<RefBookRowValue> rowValues = new ArrayList<>(rowValueCount);

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