package ru.i_novus.ms.rdm.n2o.resolver;

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
import ru.i_novus.ms.rdm.n2o.BaseTest;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.io.Serializable;
import java.math.BigInteger;
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
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateRecordGetterResolverTest extends BaseTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    private static final int TEST_REFBOOK_ID = -10;
    private static final String TEST_REFBOOK_CODE = "test";

    private static final int TEST_REFBOOK_VERSION_ID = -100;
    private static final int TEST_OPT_LOCK_VALUE = 10;

    private static final long TEST_SYSTEM_ID = 51;

    @InjectMocks
    private UpdateRecordGetterResolver resolver;

    @Mock
    private VersionRestService versionService;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_UPDATE));
        assertFalse(resolver.isSatisfied(DATA_ACTION_CREATE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateRegularValues() {

        DataRecordCriteria criteria = createCriteria();
        RefBookVersion version = createVersion();

        Map<String, Serializable> values = resolver.createRegularValues(criteria, version);
        assertNotNull(values);
        assertEquals(1, values.size());
        assertTrue(values.containsKey(FIELD_SYSTEM_ID));
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
        criteria.setDataAction(DATA_ACTION_CREATE);

        return criteria;
    }

    private RefBookVersion createVersion() {

        RefBookVersion version = new RefBookVersion();
        version.setId(TEST_REFBOOK_VERSION_ID);
        version.setOptLockValue(TEST_OPT_LOCK_VALUE);

        version.setRefBookId(TEST_REFBOOK_ID);
        version.setCode(TEST_REFBOOK_CODE);

        version.setStructure(new Structure(DEFAULT_STRUCTURE));

        return version;
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
        return searchDataCriteria;
    }
}