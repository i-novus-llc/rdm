package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
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
public class CreateRecordObjectResolverTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    private static final int TEST_REFBOOK_VERSION_ID = -10;

    private static final int TEST_ARGUMENT_COUNT = 3;
    private static final int TEST_PARAMETER_COUNT = 3;

    @InjectMocks
    private CreateRecordObjectResolver resolver;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_CREATE));
        assertFalse(resolver.isSatisfied(DATA_ACTION_UPDATE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateOperation() {

        final DataRecordRequest request = new DataRecordRequest();

        final N2oObject.Operation operation = resolver.createOperation(request);
        assertNotNull(operation);
        assertEquals("create", operation.getId());

        assertNotNull(operation.getInvocation());
        assertTrue(operation.getInvocation() instanceof N2oJavaDataProvider);

        final N2oJavaDataProvider invocation = (N2oJavaDataProvider) operation.getInvocation();
        assertEquals(TEST_ARGUMENT_COUNT, invocation.getArguments().length);
    }

    @Test
    public void testCreateRegularParams() {

        final DataRecordRequest request = new DataRecordRequest();
        request.setVersionId(TEST_REFBOOK_VERSION_ID);

        final List<AbstractParameter> parameters = resolver.createRegularParams(request);
        assertNotNull(parameters);
        assertEquals(TEST_PARAMETER_COUNT - 1, parameters.size());
    }
}