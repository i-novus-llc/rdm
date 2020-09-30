package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.N2oInvocation;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.DATA_ACTION_CREATE;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.DATA_ACTION_UPDATE;

@RunWith(MockitoJUnitRunner.class)
public class UpdateRecordObjectResolverTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    private static final int TEST_REFBOOK_VERSION_ID = -10;

    private static final int TEST_ARGUMENT_COUNT = 3;
    private static final int TEST_PARAMETER_COUNT = 4;

    @InjectMocks
    private UpdateRecordObjectResolver resolver;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_UPDATE));
        assertFalse(resolver.isSatisfied(DATA_ACTION_CREATE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateOperation() {

        DataRecordRequest request = new DataRecordRequest();
        request.setVersionId(TEST_REFBOOK_VERSION_ID);

        N2oObject.Operation operation = resolver.createOperation(request);
        assertNotNull(operation);
        assertEquals("update", operation.getId());

        N2oInvocation invocation = operation.getInvocation();
        assertNotNull(invocation);
        assertTrue(invocation instanceof N2oJavaDataProvider);

        N2oJavaDataProvider provider = (N2oJavaDataProvider) invocation;
        assertEquals(TEST_ARGUMENT_COUNT, provider.getArguments().length);
    }

    @Test
    public void testCreateRegularParams() {

        DataRecordRequest request = new DataRecordRequest();
        request.setVersionId(TEST_REFBOOK_VERSION_ID);

        List<N2oObject.Parameter> parameters = resolver.createRegularParams(request);
        assertNotNull(parameters);
        assertEquals(TEST_PARAMETER_COUNT - 1, parameters.size());
    }

    @Test
    public void testGetRecordMappingIndex() {

        DataRecordRequest request = new DataRecordRequest();

        int index = resolver.getRecordMappingIndex(request);
        assertEquals(TEST_ARGUMENT_COUNT - 1, index);
    }
}