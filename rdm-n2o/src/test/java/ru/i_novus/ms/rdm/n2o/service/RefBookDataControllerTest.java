package ru.i_novus.ms.rdm.n2o.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
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
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.n2o.BaseTest;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataDecorator;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.service.RefBookDataController.EMPTY_SEARCH_DATA_CRITERIA;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.*;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.EXACT;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.LIKE;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("SameParameterValue")
public class RefBookDataControllerTest extends BaseTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final int TEST_OPT_LOCK_VALUE = 10;
    private static final int NEW_OPT_LOCK_VALUE = TEST_OPT_LOCK_VALUE + 1;

    private static final String TEST_LOCALE_CODE = "test";

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testGetList() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        DataCriteria criteria = createCriteria(false);
        criteria.setFilter(createCriteriaFilter());
        criteria.setSorting(new Sorting(ID_ATTRIBUTE_CODE, Direction.ASC));

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize());

        List<AttributeFilter> filters = criteria.getFilter().entrySet().stream()
                .map(e -> toAttributeFilter(version.getStructure(), e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .collect(toList());
        searchDataCriteria.addAttributeFilterList(filters);

        Sort.Order order = new Sort.Order(Sort.Direction.ASC, ID_ATTRIBUTE_CODE);
        searchDataCriteria.setOrders(singletonList(order));

        List<RefBookRowValue> rowValues = createContent(TEST_REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), eq(searchDataCriteria))).thenReturn(rowValuesPage);

        when(refBookDataDecorator.getDataStructure(eq(TEST_REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());
        when(refBookDataDecorator.getDataContent(eq(rowValues), eq(criteria))).thenReturn(rowValues);

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        Page<RefBookDataController.DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // За вычетом "записи"-заголовка.
        assertEquals(rowValues.size(), dataGridRows.getContent().size() - 1);
    }

    @Test
    public void testGetListWithLocaleCode() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        DataCriteria criteria = createCriteria(false);
        criteria.setLocaleCode(TEST_LOCALE_CODE);
        criteria.setFilter(createCriteriaFilter());

        SearchDataCriteria localeDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize());
        localeDataCriteria.setLocaleCode(TEST_LOCALE_CODE);

        List<AttributeFilter> filters = criteria.getFilter().entrySet().stream()
                .map(e -> toAttributeFilter(version.getStructure(), e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .collect(toList());
        localeDataCriteria.addAttributeFilterList(filters);

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize());
        searchDataCriteria.setLocaleCode(TEST_LOCALE_CODE);
        searchDataCriteria.addAttributeFilterList(filters);

        List<RefBookRowValue> rowValues = createContent(TEST_REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, searchDataCriteria, rowValues.size());
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(new PageImpl<>(emptyList()));

        when(refBookDataDecorator.getDataStructure(eq(TEST_REFBOOK_VERSION_ID), eq(criteria))).thenReturn(version.getStructure());

        when(dataFieldFilterProvider.toFilterField(any(N2oField.class))).thenReturn(new StandardField<>());

        Page<RefBookDataController.DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // "Запись"-заголовок.
        assertEquals(1, dataGridRows.getContent().size());
    }

    @Test
    public void testGetConflictedListWithoutConflicts() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

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
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        when(conflictService.countConflictedRowIds(any(RefBookConflictCriteria.class))).thenReturn(1L);

        List<RefBookRowValue> rowValues = createContent(TEST_REFBOOK_VERSION_ID);
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(rowValues, EMPTY_SEARCH_DATA_CRITERIA, rowValues.size());
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), eq(EMPTY_SEARCH_DATA_CRITERIA))).thenReturn(rowValuesPage);

        List<Long> conflictedRowIds = List.of(1L);
        RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        when(conflictService.searchConflictedRowIds(any(RefBookConflictCriteria.class)))
                .thenReturn(new PageImpl<>(conflictedRowIds, conflictCriteria, 1));

        DataCriteria criteria = createCriteria(true);
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

        Page<RefBookDataController.DataGridRow> dataGridRows = controller.getList(criteria);
        assertNotNull(dataGridRows);
        assertNotNull(dataGridRows.getContent());
        // За вычетом "записи"-заголовка.
        assertEquals(conflictedRowValues.size(), dataGridRows.getContent().size() - 1);
    }

    @Test
    public void testGetVersion() {

        RefBookVersion version = createRefBookVersion();
        when(versionService.getById(eq(TEST_REFBOOK_VERSION_ID))).thenReturn(version);

        RefBookVersion expected = new RefBookVersion(version);

        RefBookVersion actual1 = controller.getVersion(TEST_REFBOOK_VERSION_ID, null);
        assertVersions(expected, actual1);
        assertEquals(expected.getOptLockValue(), actual1.getOptLockValue());
        assertVersions(expected, version);
        assertEquals(expected.getOptLockValue(), actual1.getOptLockValue());

        RefBookVersion actual2 = controller.getVersion(TEST_REFBOOK_VERSION_ID, NEW_OPT_LOCK_VALUE);
        assertVersions(expected, actual2);
        assertNotEquals(expected.getOptLockValue(), actual2.getOptLockValue());
        assertVersions(expected, version);
        assertNotEquals(expected.getOptLockValue(), version.getOptLockValue());
    }

    private DataCriteria createCriteria(boolean hasDataConflict) {

        DataCriteria criteria = new DataCriteria();

        criteria.setVersionId(TEST_REFBOOK_VERSION_ID);
        criteria.setOptLockValue(TEST_OPT_LOCK_VALUE);
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

    private AttributeFilter toAttributeFilter(Structure structure, String filterName, Serializable filterValue) {

        if (filterValue == null || isEmpty(filterName))
            return null;

        Structure.Attribute attribute = structure.getAttribute(filterName);
        Serializable attributeValue = castFilterValue(attribute, filterValue);
        if (attributeValue == null)
            return null;

        AttributeFilter attributeFilter = new AttributeFilter(filterName, attributeValue, attribute.getType());
        attributeFilter.setSearchType(attribute.getType() == STRING ? LIKE : EXACT);
        return attributeFilter;
    }

    private Serializable castFilterValue(Structure.Attribute attribute, Serializable value) {

        switch (attribute.getType()) {
            case INTEGER:
                return new BigInteger((String) value);

            case FLOAT:
                return new BigDecimal(((String) value).replace(",", ".").trim());

            case DATE:
                return LocalDate.parse((String) value, TimeUtils.DATE_TIME_PATTERN_EUROPEAN_FORMATTER);

            case BOOLEAN:
                if (value == null) return null;

                if ("true".equals(value)) return true;

                if ("false".equals(value)) return false;

                throw new IllegalArgumentException();

            default:
                return value;
        }
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion version = new RefBookVersion();

        version.setId(TEST_REFBOOK_VERSION_ID);
        version.setOptLockValue(TEST_OPT_LOCK_VALUE);
        version.setStructure(createStructure());

        return version;
    }

    private void assertVersions(RefBookVersion expected, RefBookVersion actual) {

        assertEquals(expected.getId(), actual.getId());
        assertObjects(Assert::assertEquals, expected.getStructure(), actual.getStructure());
    }

    /** Создание структуры с глубоким копированием атрибутов и ссылок. */
    private Structure createStructure() {

        Structure structure = new Structure(ATTRIBUTE_LIST, REFERENCE_LIST);
        return new Structure(structure);
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
                new FloatFieldValue(FLOAT_ATTRIBUTE_CODE, systemId * 1.0 / 10),
                new BooleanFieldValue(BOOLEAN_ATTRIBUTE_CODE, systemId > 10),
                new DateFieldValue(DATE_ATTRIBUTE_CODE, LocalDate.now(ZoneId.of("UTC"))),
                new ReferenceFieldValue(REFERENCE_ATTRIBUTE_CODE,
                        new Reference(String.valueOf(systemId), "display_" + systemId)),
                new ReferenceFieldValue(SELF_REFER_ATTRIBUTE_CODE,
                        new Reference(null, null))
        ));
    }
}