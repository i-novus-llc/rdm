package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.l10n.VersionLocaleService;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.test.BaseTest;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.deletePrefix;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.DATA_ACTION_LOCALIZE;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.FIELD_LOCALE_NAME;
import static ru.i_novus.ms.rdm.n2o.l10n.utils.StructureTestConstants.*;

@RunWith(MockitoJUnitRunner.class)
public class L10nLocalizeRecordGetterResolverTest extends BaseTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    private static final List<String> REGULAR_VALUE_NAMES = List.of(
            FIELD_SYSTEM_ID, FIELD_LOCALE_CODE, FIELD_LOCALE_NAME
    );

    private static final int TEST_REFBOOK_ID = -10;
    private static final String TEST_REFBOOK_CODE = "test";

    private static final int TEST_REFBOOK_VERSION_ID = -100;
    private static final int TEST_OPT_LOCK_VALUE = 10;

    private static final long TEST_SYSTEM_ID = 51;

    private static final String TEST_LOCALE_CODE = "test";
    private static final String TEST_LOCALE_NAME = "Тест";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private L10nLocalizeRecordGetterResolver resolver;

    @Mock
    private VersionRestService versionService;

    @Mock
    private VersionLocaleService versionLocaleService;

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_LOCALIZE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateRegularValues() {

        when(versionLocaleService.getLocaleName(eq(TEST_LOCALE_CODE))).thenReturn(TEST_LOCALE_NAME);

        DataRecordCriteria criteria = createCriteria();
        RefBookVersion version = createVersion();

        Map<String, Serializable> values = resolver.createRegularValues(criteria, version);
        assertNotNull(values);
        assertEquals(REGULAR_VALUE_NAMES.size(), values.size());

        REGULAR_VALUE_NAMES.forEach(name ->
                assertTrue(values.containsKey(name))
        );
    }

    @Test
    public void testCreateRegularValuesFailed() {

        DataRecordCriteria criteria = createCriteria();
        criteria.setLocaleCode(null);
        RefBookVersion version = createVersion();

        try {
            resolver.createRegularValues(criteria, version);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }

    @Test
    public void testCreateDynamicValues() {

        DataRecordCriteria criteria = createCriteria();
        RefBookVersion version = createVersion();

        RefBookRowValue rowValue = createRowValue();
        SearchDataCriteria searchDataCriteria = createSearchDataCriteria();
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(singletonList(rowValue), searchDataCriteria, 1);
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(rowValuesPage);

        Map<String, Serializable> values = resolver.createDynamicValues(criteria, version);
        assertNotNull(values);

        Structure structure = version.getStructure();
        values.forEach((fieldCode, value) ->{
            String code = deletePrefix(fieldCode);

            Structure.Attribute attribute = structure.getAttribute(code);
            assertNotNull(attribute);
        });
    }

    @Test
    public void testCreateDynamicValuesWhenEmpty() {

        DataRecordCriteria criteria = createCriteria();
        RefBookVersion version = createVersion();

        SearchDataCriteria searchDataCriteria = createSearchDataCriteria();
        Page<RefBookRowValue> rowValuesPage = new PageImpl<>(emptyList(), searchDataCriteria, 0);
        when(versionService.search(eq(TEST_REFBOOK_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(rowValuesPage);

        Map<String, Serializable> values = resolver.createDynamicValues(criteria, version);
        assertNotNull(values);
        assertEmpty(values);
    }

    private DataRecordCriteria createCriteria() {

        DataRecordCriteria criteria = new DataRecordCriteria();

        criteria.setId(TEST_SYSTEM_ID);
        criteria.setVersionId(TEST_REFBOOK_VERSION_ID);
        criteria.setOptLockValue(TEST_OPT_LOCK_VALUE);
        criteria.setLocaleCode(TEST_LOCALE_CODE);
        criteria.setDataAction(DATA_ACTION_CREATE);

        return criteria;
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

    /** Создание структуры с глубоким копированием атрибутов и ссылок. */
    private Structure createStructure() {

        Structure structure = new Structure(ATTRIBUTE_LIST, REFERENCE_LIST);
        return new Structure(structure);
    }

    private RefBookRowValue createRowValue() {

        LongRowValue longRowValue = new LongRowValue(TEST_SYSTEM_ID, asList(
                new IntegerFieldValue(ID_ATTRIBUTE_CODE, BigInteger.valueOf(TEST_SYSTEM_ID)),
                new StringFieldValue(NAME_ATTRIBUTE_CODE, "name_" + TEST_SYSTEM_ID),
                new StringFieldValue(STRING_ATTRIBUTE_CODE, "text with id = " + TEST_SYSTEM_ID)
        ));

        return new RefBookRowValue(longRowValue, TEST_REFBOOK_VERSION_ID);
    }

    private SearchDataCriteria createSearchDataCriteria() {

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(0, 1);

        searchDataCriteria.setRowSystemIds(singletonList(TEST_SYSTEM_ID));
        searchDataCriteria.setLocaleCode(TEST_LOCALE_CODE);

        return searchDataCriteria;
    }
}