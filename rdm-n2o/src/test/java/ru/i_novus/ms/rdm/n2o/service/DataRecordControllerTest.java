package ru.i_novus.ms.rdm.n2o.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.BaseTest;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordGetterResolver;
import ru.i_novus.ms.rdm.rest.client.impl.DraftRestServiceRestClient;
import ru.i_novus.ms.rdm.rest.client.impl.VersionRestServiceRestClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.DEFAULT_STRUCTURE;

@RunWith(MockitoJUnitRunner.class)
public class DataRecordControllerTest extends BaseTest {

    private static final String TEST_ACTION = "trial";

    private static final int TEST_REFBOOK_ID = -10;
    private static final String TEST_REFBOOK_CODE = "test";

    private static final int TEST_REFBOOK_DRAFT_ID = -100;
    private static final int TEST_OPT_LOCK_VALUE = 10;

    private static final long TEST_SYSTEM_ID = 51;

    private static final String TEST_FIELD_CODE = "id";

    @InjectMocks
    private DataRecordController controller;

    @Mock
    private VersionRestServiceRestClient versionService;

    @Mock
    private DraftRestServiceRestClient draftService;

    @Spy
    private final Collection<DataRecordGetterResolver> resolvers = new ArrayList<>(1);

    @Before
    public void setUp() {

        final DataRecordGetterResolver resolver = new TestRecordGetterResolver();
        resolvers.add(resolver);
    }

    @Test
    public void testGetRow() {

        final DataRecordCriteria criteria = createCriteria();

        final RefBookVersion version = createVersion();
        when(versionService.getById(eq(TEST_REFBOOK_DRAFT_ID))).thenReturn(version);

        final Map<String, Serializable> map = controller.getRow(criteria);
        assertNotNull(map);
        assertEquals(3, map.size());
    }

    @Test
    public void testGetRowFailed() {

        final DataRecordCriteria criteria = createCriteria();
        criteria.setDataAction(null);

        try {
            controller.getRow(criteria);
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }
    }

    @Test
    public void testUpdateData() {

        final Row row = createUpdatingRow();
        UpdateDataRequest expected = new UpdateDataRequest(TEST_OPT_LOCK_VALUE, singletonList(row));

        controller.updateData(TEST_REFBOOK_DRAFT_ID, TEST_OPT_LOCK_VALUE, row);

        final ArgumentCaptor<UpdateDataRequest> captor = ArgumentCaptor.forClass(UpdateDataRequest.class);
        verify(draftService, times(1)).updateData(eq(TEST_REFBOOK_DRAFT_ID), captor.capture());

        final UpdateDataRequest actual = captor.getValue();
        assertEquals(expected.getOptLockValue(), actual.getOptLockValue());
        assertEquals(expected.getRows().size(), actual.getRows().size());
        assertEquals(expected.getRows().get(0).getSystemId(), actual.getRows().get(0).getSystemId());
    }

    private DataRecordCriteria createCriteria() {

        final DataRecordCriteria criteria = new DataRecordCriteria();

        criteria.setId(TEST_SYSTEM_ID);
        criteria.setVersionId(TEST_REFBOOK_DRAFT_ID);
        criteria.setOptLockValue(TEST_OPT_LOCK_VALUE);
        criteria.setDataAction(TEST_ACTION);

        return criteria;
    }

    private RefBookVersion createVersion() {

        final RefBookVersion version = new RefBookVersion();
        version.setId(TEST_REFBOOK_DRAFT_ID);
        version.setOptLockValue(TEST_OPT_LOCK_VALUE);

        version.setRefBookId(TEST_REFBOOK_ID);
        version.setCode(TEST_REFBOOK_CODE);

        version.setStructure(createStructure());

        return version;
    }

    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }

    private Row createUpdatingRow() {

        final Map<String, Object> data = new HashMap<>(1);
        data.put(TEST_FIELD_CODE, 10);

        return new Row(1L, data);
    }

    private static class TestRecordGetterResolver implements DataRecordGetterResolver {

        @Override
        public boolean isSatisfied(String dataAction) {
            return TEST_ACTION.equals(dataAction);
        }

        @Override
        public Map<String, Serializable> createRegularValues(DataRecordCriteria criteria, RefBookVersion version) {
            return emptyMap();
        }

        @Override
        public Map<String, Serializable> createDynamicValues(DataRecordCriteria criteria, RefBookVersion version) {
            return emptyMap();
        }
    }
}