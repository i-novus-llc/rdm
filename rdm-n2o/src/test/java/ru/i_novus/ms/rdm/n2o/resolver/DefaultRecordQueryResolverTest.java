package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.DATA_ACTION_CREATE;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.DATA_ACTION_UPDATE;
import static ru.i_novus.ms.rdm.n2o.utils.UiRefBookTestUtils.assertEmptyList;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRecordQueryResolverTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    @InjectMocks
    private DefaultRecordQueryResolver resolver;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_CREATE));
        assertTrue(resolver.isSatisfied(DATA_ACTION_UPDATE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateRegularFields() {

        DataRecordRequest request = new DataRecordRequest();

        List<N2oQuery.Field> fields = resolver.createRegularFields(request);
        assertEmptyList(fields);
    }
}