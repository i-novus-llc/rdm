package ru.i_novus.ms.rdm.n2o.resolver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.BaseTest;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.io.Serializable;
import java.util.Map;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.DATA_ACTION_CREATE;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.DATA_ACTION_UPDATE;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.deletePrefix;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.DEFAULT_STRUCTURE;

@RunWith(MockitoJUnitRunner.class)
public class CreateRecordGetterResolverTest extends BaseTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    private static final int TEST_REFBOOK_ID = -10;
    private static final String TEST_REFBOOK_CODE = "test";

    private static final int TEST_REFBOOK_VERSION_ID = -100;
    private static final int TEST_OPT_LOCK_VALUE = 10;

    private static final long TEST_SYSTEM_ID = 51;

    @InjectMocks
    private CreateRecordGetterResolver resolver;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_CREATE));
        assertFalse(resolver.isSatisfied(DATA_ACTION_UPDATE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateRegularValues() {

        final DataRecordCriteria criteria = createCriteria();
        final RefBookVersion version = createVersion();

        final Map<String, Serializable> values = resolver.createRegularValues(criteria, version);
        assertNotNull(values);
        assertEmpty(values);
    }

    @Test
    public void testCreateDynamicValues() {

        final DataRecordCriteria criteria = createCriteria();
        final RefBookVersion version = createVersion();

        final Map<String, Serializable> values = resolver.createDynamicValues(criteria, version);
        assertNotNull(values);

        final Structure structure = version.getStructure();
        values.forEach((fieldCode, value) ->{

            final String code = deletePrefix(fieldCode);

            final Structure.Attribute attribute = structure.getAttribute(code);
            assertNotNull(attribute);

            final Structure.Reference reference = structure.getReference(code);
            if (reference != null) {
                assertEquals(new Reference(), value);
            }
        });
    }

    private DataRecordCriteria createCriteria() {

        final DataRecordCriteria criteria = new DataRecordCriteria();
        criteria.setId(TEST_SYSTEM_ID);
        criteria.setVersionId(TEST_REFBOOK_VERSION_ID);
        criteria.setOptLockValue(TEST_OPT_LOCK_VALUE);
        criteria.setDataAction(DATA_ACTION_CREATE);

        return criteria;
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

    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }
}