package ru.i_novus.ms.rdm.n2o.l10n.resolver;

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
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.DATA_ACTION_LOCALIZE;

@RunWith(MockitoJUnitRunner.class)
public class L10nLocalizeRecordObjectResolverTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    private static final int TEST_REFBOOK_VERSION_ID = -10;

    private static final int TEST_ARGUMENT_COUNT = 4;
    private static final int TEST_PARAMETER_COUNT = 5;

    @InjectMocks
    private L10nLocalizeRecordObjectResolver resolver;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_LOCALIZE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateOperation() {

        DataRecordRequest request = new DataRecordRequest();
        request.setVersionId(TEST_REFBOOK_VERSION_ID);

        N2oObject.Operation operation = resolver.createOperation(request);
        assertNotNull(operation);
        assertEquals("localize", operation.getId());

        assertNotNull(operation.getInvocation());
        assertTrue(operation.getInvocation() instanceof N2oJavaDataProvider);

        N2oJavaDataProvider invocation = (N2oJavaDataProvider) operation.getInvocation();
        assertEquals(TEST_ARGUMENT_COUNT, invocation.getArguments().length);
    }

    @Test
    public void testCreateRegularParams() {

        DataRecordRequest request = new DataRecordRequest();
        request.setVersionId(TEST_REFBOOK_VERSION_ID);

        List<AbstractParameter> parameters = resolver.createRegularParams(request);
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